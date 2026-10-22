package com.ambulanceservice.dispatch.dto;

import com.ambulanceservice.user.entity.User;

public record AvailableDriverResponse(
    Long id,
    String fullName,
    String email,
    String phone,
    String address,
    String ambulanceRegistrationNumber
) {
  public static AvailableDriverResponse fromEntity(User user) {
    return new AvailableDriverResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getPhone(),
        user.getAddress(),
        user.getAmbulanceRegistrationNumber()
    );
  }
}
