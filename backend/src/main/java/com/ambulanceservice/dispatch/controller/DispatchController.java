package com.ambulanceservice.dispatch.controller;

import com.ambulanceservice.booking.dto.BookingResponse;
import com.ambulanceservice.booking.entity.BookingChannel;
import com.ambulanceservice.booking.service.BookingService;
import com.ambulanceservice.booking.dto.CreateBookingRequest;
import com.ambulanceservice.dispatch.dto.AssignDriverRequest;
import com.ambulanceservice.dispatch.dto.AvailableDriverResponse;
import com.ambulanceservice.dispatch.dto.PhoneBookingRequest;
import com.ambulanceservice.dispatch.service.DriverDispatchService;
import com.ambulanceservice.dispatch.dto.DispatchDriverCandidateResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/dispatch")
@RequiredArgsConstructor
public class DispatchController {

  private final BookingService bookingService;
  private final DriverDispatchService driverDispatchService;

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
        request.pickupLatitude(),
        request.pickupLongitude(),
        request.destinationHospital() != null ? request.destinationHospital() : "Nearest General Hospital",
        request.destinationLatitude(),
        request.destinationLongitude(),
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

  @GetMapping("/drivers/nearest")
  @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<List<DispatchDriverCandidateResponse>> getNearestAvailableDrivers(
      @RequestParam @DecimalMin(value = "-90.0", inclusive = true, message = "Pickup latitude must be between -90 and 90")
      @DecimalMax(value = "90.0", inclusive = true, message = "Pickup latitude must be between -90 and 90") Double pickupLatitude,
      @RequestParam @DecimalMin(value = "-180.0", inclusive = true, message = "Pickup longitude must be between -180 and 180")
      @DecimalMax(value = "180.0", inclusive = true, message = "Pickup longitude must be between -180 and 180") Double pickupLongitude,
      @RequestParam @DecimalMin(value = "-90.0", inclusive = true, message = "Destination latitude must be between -90 and 90")
      @DecimalMax(value = "90.0", inclusive = true, message = "Destination latitude must be between -90 and 90") Double destinationLatitude,
      @RequestParam @DecimalMin(value = "-180.0", inclusive = true, message = "Destination longitude must be between -180 and 180")
      @DecimalMax(value = "180.0", inclusive = true, message = "Destination longitude must be between -180 and 180") Double destinationLongitude) {
    return ResponseEntity.ok(driverDispatchService.getNearestAvailableDrivers(
        pickupLatitude,
        pickupLongitude,
        destinationLatitude,
        destinationLongitude
    ));
  }
}
