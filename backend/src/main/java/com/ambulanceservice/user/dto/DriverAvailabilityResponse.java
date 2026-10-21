package com.ambulanceservice.user.dto;

import com.ambulanceservice.user.entity.DriverAvailabilityStatus;
import com.ambulanceservice.user.entity.User;

import java.time.LocalDateTime;

public record DriverAvailabilityResponse(
    Long id,
    String fullName,
    String email,
    DriverAvailabilityStatus availabilityStatus,
    Double latitude,
    Double longitude,
    LocalDateTime lastLocationUpdate
) {
  public static DriverAvailabilityResponse fromEntity(User user) {
    return new DriverAvailabilityResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getAvailabilityStatus(),
        user.getLatitude(),
        user.getLongitude(),
        user.getLastLocationUpdate()
    );
  }
}

