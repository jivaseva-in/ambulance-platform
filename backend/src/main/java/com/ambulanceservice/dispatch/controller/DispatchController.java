package com.ambulanceservice.dispatch.controller;

import com.ambulanceservice.booking.dto.BookingResponse;
import com.ambulanceservice.booking.entity.BookingChannel;
import com.ambulanceservice.booking.service.BookingService;
import com.ambulanceservice.booking.dto.CreateBookingRequest;
import com.ambulanceservice.dispatch.dto.AssignDriverRequest;
import com.ambulanceservice.dispatch.dto.AvailableDriverResponse;
import com.ambulanceservice.dispatch.dto.PhoneBookingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatch")
@RequiredArgsConstructor
public class DispatchController {

  private final BookingService bookingService;

  @PostMapping("/phone-call")
  @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN', 'SUPER_ADMIN')")
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

    BookingResponse response = bookingService.createBooking(bookingRequest, dispatcher.getUsername(), false);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/bookings/open")
  @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<List<BookingResponse>> getOpenDispatchBookings() {
    return ResponseEntity.ok(bookingService.getOpenDispatchBookings());
  }

  @GetMapping("/drivers/available")
  @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<List<AvailableDriverResponse>> getAvailableDrivers() {
    return ResponseEntity.ok(bookingService.getAvailableDriversForDispatch());
  }

  @PutMapping("/bookings/{bookingId}/assign")
  @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<BookingResponse> assignDriver(
      @PathVariable Long bookingId,
      @Valid @RequestBody AssignDriverRequest request,
      @AuthenticationPrincipal UserDetails dispatcher) {
    return ResponseEntity.ok(bookingService.assignDriver(bookingId, request, dispatcher.getUsername()));
  }

  @PutMapping("/bookings/{bookingId}/unassign")
  @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<BookingResponse> unassignDriver(
      @PathVariable Long bookingId,
      @AuthenticationPrincipal UserDetails dispatcher) {
    return ResponseEntity.ok(bookingService.unassignDriver(bookingId, dispatcher.getUsername()));
  }
}
