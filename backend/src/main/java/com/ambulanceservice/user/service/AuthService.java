package com.ambulanceservice.user.service;

import com.ambulanceservice.security.JwtUtils;
import com.ambulanceservice.user.dto.*;
import com.ambulanceservice.user.entity.Role;
import com.ambulanceservice.user.entity.UserStatus;
import com.ambulanceservice.user.entity.User;
import com.ambulanceservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtUtils jwtUtils;
  private final UserDetailsService userDetailsService;

  @Value("${app.registration.admin-invite-code:}")
  private String adminInviteCode;

  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new IllegalArgumentException("Email is already registered");
    }
    if (userRepository.existsByPhone(request.phone())) {
      throw new IllegalArgumentException("Phone number is already registered");
    }

    if (!request.password().equals(request.confirmPassword())) {
      throw new IllegalArgumentException("Passwords do not match");
    }

    // Prevent self-registration as SUPER_ADMIN
    if (request.role() == Role.ROLE_SUPER_ADMIN) {
      throw new IllegalArgumentException("Cannot register as Super Admin.");
    }

    if (request.role() == Role.ROLE_DRIVER && (request.ambulanceRegistrationNumber() == null || request.ambulanceRegistrationNumber().isBlank())) {
      throw new IllegalArgumentException("Ambulance registration number is required for drivers.");
    }
    if (request.role() == Role.ROLE_DRIVER && userRepository.existsByAmbulanceRegistrationNumber(request.ambulanceRegistrationNumber().trim())) {
      throw new IllegalArgumentException("Ambulance registration number is already registered to another driver.");
    }

    if ((request.role() == Role.ROLE_DRIVER || request.role() == Role.ROLE_DISPATCHER)
        && (request.address() == null || request.address().isBlank())) {
      throw new IllegalArgumentException("Address is required for drivers and dispatchers");
    }

    if (request.role() == Role.ROLE_ADMIN) {
      if (adminInviteCode == null || adminInviteCode.isBlank()) {
        throw new IllegalStateException("Admin registration is not configured");
      }

      if (request.inviteCode() == null || request.inviteCode().isBlank()) {
        throw new IllegalArgumentException("Invitation code is required for admin registration");
      }

      if (!adminInviteCode.equals(request.inviteCode().trim())) {
        throw new IllegalArgumentException("Invalid admin invitation code");
      }
    }

    UserStatus initialStatus = determineInitialStatus(request.role());

    User user = User.builder()
        .fullName(request.fullName().trim())
        .email(request.email().trim())
        .phone(request.phone().trim())
        .address(request.address() != null ? request.address().trim() : null)
        .ambulanceRegistrationNumber(request.role() == Role.ROLE_DRIVER ? request.ambulanceRegistrationNumber().trim() : null)
        .password(passwordEncoder.encode(request.password()))
        .role(request.role())
        .status(initialStatus)
        .build();

    userRepository.save(user);

    // Only generate a token if the user is active
    if (initialStatus == UserStatus.ACTIVE) {
      UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
      String token = jwtUtils.generateToken(userDetails);
      return AuthResponse.of(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    // For non-active users, return a null token and a message
    return AuthResponse.of(null, user.getId(), user.getFullName(), user.getEmail(), user.getRole());
  }

  public AuthResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.username())
        .or(() -> userRepository.findByPhone(request.username()))
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new DisabledException(switch (user.getStatus()) {
        case PENDING -> "Your account is pending approval from an administrator.";
        case SUSPENDED -> "Your account has been suspended.";
        case REJECTED -> "Your account registration was rejected.";
        case ACTIVE -> "Your account is active.";
      });
    }

    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password())
    );

    SecurityContextHolder.getContext().setAuthentication(authentication);

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String token = jwtUtils.generateToken(userDetails);

    return AuthResponse.of(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole());
  }

  private UserStatus determineInitialStatus(Role role) {
    return switch (role) {
      case ROLE_USER, ROLE_ADMIN, ROLE_SUPER_ADMIN -> UserStatus.ACTIVE;
      case ROLE_DRIVER, ROLE_DISPATCHER -> UserStatus.PENDING;
    };
  }
}
