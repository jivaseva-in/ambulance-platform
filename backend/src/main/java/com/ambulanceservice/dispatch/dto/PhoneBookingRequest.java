package com.ambulanceservice.dispatch.dto;

import com.ambulanceservice.booking.entity.AmbulanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PhoneBookingRequest(
    @NotBlank(message = "Caller contact name is required")
    String callerName,

    @NotBlank(message = "Caller phone number is required")
    String callerPhone,

    @NotBlank(message = "Pickup location is required")
    String pickupAddress,

    Double pickupLatitude,
    Double pickupLongitude,

    String destinationHospital,
    Double destinationLatitude,
    Double destinationLongitude,

    @NotBlank(message = "Patient triage condition is required")
    String triageNotes,

    @NotNull(message = "Ambulance vehicle category is required")
    AmbulanceType ambulanceType,

    String dispatcherInternalNotes
) {}
