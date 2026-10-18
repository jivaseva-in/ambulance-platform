package com.ambulanceservice.user.service;

import com.ambulanceservice.common.ResourceNotFoundException;
import com.ambulanceservice.user.dto.UserResponse;
import com.ambulanceservice.user.entity.Role;
import com.ambulanceservice.user.entity.UserStatus;
import com.ambulanceservice.user.entity.User;
import com.ambulanceservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public List<UserResponse> getPendingApprovalUsers(String approverEmail) {
    User approver = getUserByEmail(approverEmail);
    EnumSet<Role> manageableRoles = getManageableRoles(approver.getRole());

    return userRepository.findByStatus(UserStatus.PENDING).stream()
        .filter(user -> manageableRoles.contains(user.getRole()))
        .map(UserResponse::fromEntity)
        .collect(Collectors.toList());
  }

  @Transactional
  public UserResponse approveUser(Long userId, String approverEmail) {
    User approver = getUserByEmail(approverEmail);
    User user = getManageableUser(userId, approver, "approve");
    ensureStatus(user, UserStatus.PENDING, "Only pending users can be approved.");
    return updateUserStatus(user, UserStatus.ACTIVE);
  }

  @Transactional
  public UserResponse rejectUser(Long userId, String approverEmail) {
    User approver = getUserByEmail(approverEmail);
    User user = getManageableUser(userId, approver, "reject");
    ensureStatus(user, UserStatus.PENDING, "Only pending users can be rejected.");
    return updateUserStatus(user, UserStatus.REJECTED);
  }

  @Transactional
  public UserResponse suspendUser(Long userId, String approverEmail) {
    User approver = getUserByEmail(approverEmail);
    User user = getManageableUser(userId, approver, "suspend");
    ensureStatus(user, UserStatus.ACTIVE, "Only active users can be suspended.");
    return updateUserStatus(user, UserStatus.SUSPENDED);
  }

  @Transactional
  public UserResponse reactivateUser(Long userId, String approverEmail) {
    User approver = getUserByEmail(approverEmail);
    User user = getManageableUser(userId, approver, "reactivate");
    ensureStatus(user, UserStatus.SUSPENDED, "Only suspended users can be reactivated.");
    return updateUserStatus(user, UserStatus.ACTIVE);
  }

  public List<UserResponse> getManageableUsers(String approverEmail) {
    User approver = getUserByEmail(approverEmail);
    EnumSet<Role> manageableRoles = getManageableRoles(approver.getRole());

    return userRepository.findAll().stream()
        .filter(user -> manageableRoles.contains(user.getRole()))
        .map(UserResponse::fromEntity)
        .collect(Collectors.toList());
  }

  private User getUserByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
  }

  private UserResponse updateUserStatus(User user, UserStatus targetStatus) {
    user.setStatus(targetStatus);
    userRepository.save(user);
    return UserResponse.fromEntity(user);
  }

  private User getManageableUser(Long userId, User approver, String action) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    if (!getManageableRoles(approver.getRole()).contains(user.getRole())) {
      throw new AccessDeniedException("You are not allowed to " + action + " users with role: " + user.getRole());
    }

    return user;
  }

  private void ensureStatus(User user, UserStatus expectedStatus, String message) {
    if (user.getStatus() != expectedStatus) {
      throw new IllegalStateException(message);
    }
  }

  private EnumSet<Role> getManageableRoles(Role approverRole) {
    return switch (approverRole) {
      case ROLE_ADMIN -> EnumSet.of(Role.ROLE_DRIVER, Role.ROLE_DISPATCHER);
      case ROLE_SUPER_ADMIN -> EnumSet.of(Role.ROLE_DRIVER, Role.ROLE_DISPATCHER, Role.ROLE_ADMIN);
      default -> throw new AccessDeniedException("You are not allowed to approve users.");
    };
  }
}
