package com.ambulanceservice.dispatch.dto;

import jakarta.validation.constraints.NotNull;

public record AssignDriverRequest(
    @NotNull(message = "Driver ID is required")
    Long driverId
) {}

