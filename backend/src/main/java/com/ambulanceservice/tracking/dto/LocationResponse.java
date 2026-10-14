package com.ambulanceservice.tracking.dto;

import java.time.LocalDateTime;

public record LocationResponse(
    Long bookingId,
    String status,
    Double driverLatitude,
    Double driverLongitude,
    LocalDateTime lastUpdated,
    String driverName,
    String driverPhone
) {}
