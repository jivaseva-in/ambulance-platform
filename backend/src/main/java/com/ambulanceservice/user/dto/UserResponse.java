package com.ambulanceservice.user.dto;

import com.ambulanceservice.user.entity.Role;
import com.ambulanceservice.user.entity.UserStatus;
import com.ambulanceservice.user.entity.User;
import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String fullName,
    String email,
    String phone,
    Role role,
    UserStatus status,
    LocalDateTime createdAt
) {
  public static UserResponse fromEntity(User user) {
    return new UserResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getPhone(),
        user.getRole(),
        user.getStatus(),
        user.getCreatedAt()
    );
  }
}
