package com.ambulanceservice.user.dto;

import com.ambulanceservice.user.entity.DriverAvailabilityStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record DriverAvailabilityRequest(
    @NotNull(message = "Availability status is required")
    DriverAvailabilityStatus availabilityStatus,

    @DecimalMin(value = "-90.0", inclusive = true, message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", inclusive = true, message = "Latitude must be between -90 and 90")
    Double latitude,

    @DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", inclusive = true, message = "Longitude must be between -180 and 180")
    Double longitude
) {}
