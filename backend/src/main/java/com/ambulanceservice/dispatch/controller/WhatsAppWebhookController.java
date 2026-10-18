package com.ambulanceservice.dispatch.controller;

import com.ambulanceservice.booking.dto.BookingResponse;
import com.ambulanceservice.booking.dto.CreateBookingRequest;
import com.ambulanceservice.booking.entity.AmbulanceType;
import com.ambulanceservice.booking.entity.BookingChannel;
import com.ambulanceservice.booking.service.BookingService;
import com.ambulanceservice.dispatch.dto.WhatsAppWebhookPayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks/whatsapp")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

  private final BookingService bookingService;

  /**
   * Webhook endpoint called by WhatsApp Business API / Twilio gateway.
   * Publicly accessible as incoming webhooks use signature verification.
   */
  @PostMapping
  public ResponseEntity<BookingResponse> handleIncomingWhatsApp(@Valid @RequestBody WhatsAppWebhookPayload payload) {

    String pickupAddress = payload.addressText() != null ? payload.addressText() : "Shared WhatsApp GPS Pin";
    Double lat = payload.latitude() != null ? payload.latitude() : 0.0;
    Double lng = payload.longitude() != null ? payload.longitude() : 0.0;

    CreateBookingRequest bookingRequest = new CreateBookingRequest(
        payload.senderName(),
        payload.fromNumber(),
        pickupAddress,
        lat,
        lng,
        "Emergency Trauma Center (Auto-routed)",
        0.0,
        0.0,
        "WhatsApp Message: " + (payload.messageBody() != null ? payload.messageBody() : "Location Shared"),
        AmbulanceType.BASIC_LIFE_SUPPORT,
        BookingChannel.WHATSAPP
    );

    // Created anonymously under WHATSAPP channel
    BookingResponse response = bookingService.createBooking(bookingRequest, null, false);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
