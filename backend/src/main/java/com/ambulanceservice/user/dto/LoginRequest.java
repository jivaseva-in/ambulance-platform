package com.ambulanceservice.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Email or phone identifier is required")
    String username,

    @NotBlank(message = "Password is required")
    String password
) {}
