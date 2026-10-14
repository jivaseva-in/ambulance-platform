package com.ambulanceservice.dispatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WhatsAppWebhookPayload(
    @NotBlank(message = "Sender WhatsApp number is required")
    String fromNumber,

    @NotBlank(message = "Sender profile name is required")
    String senderName,

    String messageBody,

    // Coordinates sent via WhatsApp location sharing
    Double latitude,
    Double longitude,
    String addressText
) {}
