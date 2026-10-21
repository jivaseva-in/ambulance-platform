package com.ambulanceservice.dispatch.dto;

import com.ambulanceservice.user.entity.DriverAvailabilityStatus;
import com.ambulanceservice.user.entity.User;

import java.time.LocalDateTime;

public record DispatchDriverCandidateResponse(
    Long id,
    String fullName,
    String email,
    String phone,
    String address,
    DriverAvailabilityStatus availabilityStatus,
    Double latitude,
    Double longitude,
    LocalDateTime lastLocationUpdate,
    Double distanceKm
) {
  public static DispatchDriverCandidateResponse fromEntity(User user, Double distanceKm) {
    return new DispatchDriverCandidateResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getPhone(),
        user.getAddress(),
        user.getAvailabilityStatus(),
        user.getLatitude(),
        user.getLongitude(),
        user.getLastLocationUpdate(),
        distanceKm
    );
  }
}

