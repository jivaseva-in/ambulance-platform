package com.ambulanceservice.tracking.dto;

import jakarta.validation.constraints.NotNull;

public record LocationUpdateRequest(
    @NotNull(message = "Latitude is required")
    Double latitude,

    @NotNull(message = "Longitude is required")
    Double longitude,

    Double speedKmh,
    Double headingDegrees
) {}