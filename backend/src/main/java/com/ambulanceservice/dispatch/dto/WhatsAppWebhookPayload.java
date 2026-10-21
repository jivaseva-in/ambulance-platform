package com.ambulanceservice.dispatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record WhatsAppWebhookPayload(
    @NotBlank(message = "Sender WhatsApp number is required")
    String fromNumber,

    @NotBlank(message = "Sender profile name is required")
    String senderName,

    String messageBody,

    // Coordinates sent via WhatsApp location sharing
    @DecimalMin(value = "-90.0", inclusive = true, message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", inclusive = true, message = "Latitude must be between -90 and 90")
    Double latitude,

    @DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", inclusive = true, message = "Longitude must be between -180 and 180")
    Double longitude,

    String addressText
) {}
