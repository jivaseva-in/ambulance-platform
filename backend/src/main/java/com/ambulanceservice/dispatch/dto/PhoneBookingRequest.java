package com.ambulanceservice.dispatch.dto;

import com.ambulanceservice.booking.entity.AmbulanceType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PhoneBookingRequest(
    @NotBlank(message = "Caller contact name is required")
    String callerName,

    @NotBlank(message = "Caller phone number is required")
    String callerPhone,

    @NotBlank(message = "Pickup location is required")
    String pickupAddress,

    @DecimalMin(value = "-90.0", inclusive = true, message = "Pickup latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", inclusive = true, message = "Pickup latitude must be between -90 and 90")
    Double pickupLatitude,

    @DecimalMin(value = "-180.0", inclusive = true, message = "Pickup longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", inclusive = true, message = "Pickup longitude must be between -180 and 180")
    Double pickupLongitude,

    String destinationHospital,

    @DecimalMin(value = "-90.0", inclusive = true, message = "Destination latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", inclusive = true, message = "Destination latitude must be between -90 and 90")
    Double destinationLatitude,

    @DecimalMin(value = "-180.0", inclusive = true, message = "Destination longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", inclusive = true, message = "Destination longitude must be between -180 and 180")
    Double destinationLongitude,

    @NotBlank(message = "Patient triage condition is required")
    String triageNotes,

    @NotNull(message = "Ambulance vehicle category is required")
    AmbulanceType ambulanceType,

    String dispatcherInternalNotes
) {}
