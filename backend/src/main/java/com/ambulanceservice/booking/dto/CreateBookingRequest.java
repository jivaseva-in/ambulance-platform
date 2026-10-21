package com.ambulanceservice.booking.dto;

import com.ambulanceservice.booking.entity.AmbulanceType;
import com.ambulanceservice.booking.entity.BookingChannel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
    @NotBlank(message = "Contact name is required")
    String contactName,

    @NotBlank(message = "Contact phone is required")
    String contactPhone,

    @NotBlank(message = "Pickup address is required")
    String pickupAddress,

    @DecimalMin(value = "-90.0", inclusive = true, message = "Pickup latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", inclusive = true, message = "Pickup latitude must be between -90 and 90")
    Double pickupLatitude,

    @DecimalMin(value = "-180.0", inclusive = true, message = "Pickup longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", inclusive = true, message = "Pickup longitude must be between -180 and 180")
    Double pickupLongitude,

    @NotBlank(message = "Destination address is required")
    String destinationAddress,

    @DecimalMin(value = "-90.0", inclusive = true, message = "Destination latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", inclusive = true, message = "Destination latitude must be between -90 and 90")
    Double destinationLatitude,

    @DecimalMin(value = "-180.0", inclusive = true, message = "Destination longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", inclusive = true, message = "Destination longitude must be between -180 and 180")
    Double destinationLongitude,

    String patientCondition,

    @NotNull(message = "Ambulance type is required")
    AmbulanceType ambulanceType,

    @NotNull(message = "Booking channel is required")
    BookingChannel bookingChannel
) {}
