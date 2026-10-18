package com.ambulanceservice.user.dto;

import com.ambulanceservice.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Full name is required")
    String fullName,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Phone number is required")
    @Size(min = 8, max = 15, message = "Phone must be between 8 and 15 digits")
    String phone,

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password,

    @NotBlank(message = "Confirm password is required")
    String confirmPassword,

    @NotNull(message = "Role is required")
    Role role,

    @Size(max = 255, message = "Address must be 255 characters or fewer")
    String address,

    @Size(max = 120, message = "Invitation code must be 120 characters or fewer")
    String inviteCode
) {}
