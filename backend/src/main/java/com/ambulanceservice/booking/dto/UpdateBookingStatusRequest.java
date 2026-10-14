package com.ambulanceservice.booking.dto;

import com.ambulanceservice.booking.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateBookingStatusRequest(
    @NotNull(message = "Target status is required")
    BookingStatus status,
    String cancellationReason
) {}
