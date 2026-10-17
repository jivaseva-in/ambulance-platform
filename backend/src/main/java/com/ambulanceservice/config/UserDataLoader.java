package com.ambulanceservice.config;

import com.ambulanceservice.user.entity.Role;
import com.ambulanceservice.user.entity.UserStatus;
import com.ambulanceservice.user.entity.User;
import com.ambulanceservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDataLoader implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  // These values will be injected from your application.properties or environment variables
  @Value("${app.superadmin.email}")
  private String superAdminEmail;

  @Value("${app.superadmin.password}")
  private String superAdminPassword;

  @Value("${app.superadmin.phone}")
  private String superAdminPhone;

  @Override
  public void run(String... args) {
    // Check if a SUPER_ADMIN already exists
    if (userRepository.existsByRole(Role.ROLE_SUPER_ADMIN)) {
      log.info("Super admin user already exists. Skipping creation.");
      return;
    }

    // Create the SUPER_ADMIN user
    User superAdmin = User.builder()
        .fullName("Super Admin")
        .email(superAdminEmail)
        .phone(superAdminPhone)
        .password(passwordEncoder.encode(superAdminPassword))
        .role(Role.ROLE_SUPER_ADMIN)
        .status(UserStatus.ACTIVE)
        .build();

    userRepository.save(superAdmin);
    log.info("Super admin user created successfully with email: {}", superAdminEmail);
  }
}
