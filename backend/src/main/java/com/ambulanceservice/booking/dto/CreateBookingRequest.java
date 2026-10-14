package com.ambulanceservice.booking.dto;

import com.ambulanceservice.booking.entity.AmbulanceType;
import com.ambulanceservice.booking.entity.BookingChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
    @NotBlank(message = "Contact name is required")
    String contactName,

    @NotBlank(message = "Contact phone is required")
    String contactPhone,

    @NotBlank(message = "Pickup address is required")
    String pickupAddress,

    Double pickupLatitude,
    Double pickupLongitude,

    @NotBlank(message = "Destination address is required")
    String destinationAddress,

    Double destinationLatitude,
    Double destinationLongitude,

    String patientCondition,

    @NotNull(message = "Ambulance type is required")
    AmbulanceType ambulanceType,

    @NotNull(message = "Booking channel is required")
    BookingChannel bookingChannel
) {}
