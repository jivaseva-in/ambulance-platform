package com.ambulanceservice.user.service;

import com.ambulanceservice.common.ResourceNotFoundException;
import com.ambulanceservice.user.dto.UserResponse;
import com.ambulanceservice.user.dto.DriverAvailabilityRequest;
import com.ambulanceservice.user.dto.DriverAvailabilityResponse;
import com.ambulanceservice.user.dto.DriverLocationUpdateRequest;
import com.ambulanceservice.user.entity.Role;
import com.ambulanceservice.user.entity.UserStatus;
import com.ambulanceservice.user.entity.User;
import com.ambulanceservice.user.entity.DriverAvailabilityStatus;
import com.ambulanceservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  @Value("${app.driver.availability.max-stale-minutes:1440}")
  private long maxStaleMinutes;

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

  @Transactional
  public DriverAvailabilityResponse updateDriverAvailability(String driverEmail, DriverAvailabilityRequest request) {
    User driver = getUserByEmail(driverEmail);

    if (driver.getRole() != Role.ROLE_DRIVER) {
      throw new IllegalStateException("Only drivers can update availability.");
    }

    if (driver.getStatus() != UserStatus.ACTIVE) {
      throw new IllegalStateException("Only approved drivers can change availability status.");
    }

    if (request.availabilityStatus() == DriverAvailabilityStatus.AVAILABLE) {
      if (request.latitude() != null || request.longitude() != null) {
        if (request.latitude() == null || request.longitude() == null) {
          throw new IllegalArgumentException("Latitude and longitude must both be provided together when updating a driver's location.");
        }
        validateCoordinates(request.latitude(), request.longitude(), "Latitude and longitude are invalid for an available driver.");
        driver.setLatitude(request.latitude());
        driver.setLongitude(request.longitude());
        driver.setLastLocationUpdate(LocalDateTime.now());
      } else {
        driver.setLatitude(null);
        driver.setLongitude(null);
        driver.setLastLocationUpdate(LocalDateTime.now());
      }
    } else {
      driver.setLatitude(null);
      driver.setLongitude(null);
      driver.setLastLocationUpdate(LocalDateTime.now());
    }

    driver.setAvailabilityStatus(request.availabilityStatus());
    userRepository.save(driver);
    return DriverAvailabilityResponse.fromEntity(driver);
  }

  @Transactional
  public DriverAvailabilityResponse updateDriverLocation(String driverEmail, DriverLocationUpdateRequest request) {
    User driver = getUserByEmail(driverEmail);

    if (driver.getRole() != Role.ROLE_DRIVER) {
      throw new IllegalStateException("Only drivers can update their location.");
    }
    if (driver.getStatus() != UserStatus.ACTIVE) {
      throw new IllegalStateException("Only approved drivers can update their location.");
    }
    if (driver.getAvailabilityStatus() != DriverAvailabilityStatus.AVAILABLE) {
      throw new IllegalStateException("Driver must be marked available before updating location.");
    }

    validateCoordinates(request.latitude(), request.longitude(), "Location coordinates are invalid.");
    driver.setLatitude(request.latitude());
    driver.setLongitude(request.longitude());
    driver.setLastLocationUpdate(LocalDateTime.now());
    userRepository.save(driver);

    return DriverAvailabilityResponse.fromEntity(driver);
  }

  public DriverAvailabilityResponse getDriverAvailability(String driverEmail) {
    User driver = getUserByEmail(driverEmail);
    if (driver.getRole() != Role.ROLE_DRIVER) {
      throw new IllegalStateException("Only drivers can check availability status.");
    }
    return DriverAvailabilityResponse.fromEntity(driver);
  }

  public boolean isDriverAvailableForDispatch(User driver) {
    if (driver.getRole() != Role.ROLE_DRIVER) {
      return false;
    }
    if (driver.getStatus() != UserStatus.ACTIVE) {
      return false;
    }
    if (driver.getAvailabilityStatus() != DriverAvailabilityStatus.AVAILABLE) {
      return false;
    }
    if (driver.getLatitude() == null || driver.getLongitude() == null) {
      return false;
    }
    if (driver.getLastLocationUpdate() == null) {
      return false;
    }
    return ChronoUnit.MINUTES.between(driver.getLastLocationUpdate(), LocalDateTime.now()) <= maxStaleMinutes;
  }

  private void validateCoordinates(Double latitude, Double longitude, String message) {
    if (latitude == null || longitude == null) {
      throw new IllegalArgumentException(message);
    }
    if (latitude.isNaN() || longitude.isNaN() || latitude.isInfinite() || longitude.isInfinite()) {
      throw new IllegalArgumentException(message);
    }
    if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
      throw new IllegalArgumentException(message);
    }
  }
}
