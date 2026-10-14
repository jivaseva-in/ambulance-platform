package com.ambulanceservice.user.dto;

import com.ambulanceservice.user.entity.Role;

public record AuthResponse(
    String token,
    String tokenType,
    Long userId,
    String fullName,
    String email,
    Role role
) {
  public static AuthResponse of(String token, Long userId, String fullName, String email, Role role) {
    return new AuthResponse(token, "Bearer", userId, fullName, email, role);
  }
}
