package com.ambulanceservice.dispatch.controller;

import com.ambulanceservice.booking.dto.BookingResponse;
import com.ambulanceservice.booking.entity.BookingChannel;
import com.ambulanceservice.booking.service.BookingService;
import com.ambulanceservice.booking.dto.CreateBookingRequest;
import com.ambulanceservice.dispatch.dto.PhoneBookingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dispatch")
@RequiredArgsConstructor
public class DispatchController {

  private final BookingService bookingService;

  @PostMapping("/phone-call")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<BookingResponse> createPhoneBooking(
      @Valid @RequestBody PhoneBookingRequest request,
      @AuthenticationPrincipal UserDetails dispatcher) {

    // Adapt phone intake to core domain booking with PHONE_DISPATCH channel
    CreateBookingRequest bookingRequest = new CreateBookingRequest(
        request.callerName(),
        request.callerPhone(),
        request.pickupAddress(),
        request.pickupLatitude() != null ? request.pickupLatitude() : 0.0,
        request.pickupLongitude() != null ? request.pickupLongitude() : 0.0,
        request.destinationHospital() != null ? request.destinationHospital() : "Nearest General Hospital",
        request.destinationLatitude() != null ? request.destinationLatitude() : 0.0,
        request.destinationLongitude() != null ? request.destinationLongitude() : 0.0,
        "[Phone Call Dispatch: " + dispatcher.getUsername() + "] " + request.triageNotes()
            + (request.dispatcherInternalNotes() != null ? " | Notes: " + request.dispatcherInternalNotes() : ""),
        request.ambulanceType(),
        BookingChannel.PHONE_DISPATCH
    );

    BookingResponse response = bookingService.createBooking(bookingRequest, dispatcher.getUsername());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
