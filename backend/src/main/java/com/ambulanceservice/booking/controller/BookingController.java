package com.ambulanceservice.booking.controller;

import com.ambulanceservice.booking.dto.*;
import com.ambulanceservice.booking.service.BookingService;
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
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

  private final BookingService bookingService;

  // 1. Create a new emergency booking (PUBLIC / ANONYMOUS or AUTHENTICATED)
  @PostMapping
  public ResponseEntity<BookingResponse> createBooking(
      @Valid @RequestBody CreateBookingRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    String email = userDetails != null ? userDetails.getUsername() : null;
    return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request, email, true));
  }

  // 2. Track booking details by ID (PUBLIC: Allows patient/guest to monitor their assigned ambulance)
  @GetMapping("/{id}")
  public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
    return ResponseEntity.ok(bookingService.getBookingById(id));
  }

  // 2a. Track booking details by phone number (PUBLIC: Allows patient/guest to monitor by contact phone)
  @GetMapping("/phone/{phoneNumber}")
  public ResponseEntity<BookingResponse> getBookingByPhone(@PathVariable String phoneNumber) {
    return ResponseEntity.ok(bookingService.getBookingByPhone(phoneNumber));
  }

  // 3. Driver feeds: Get unassigned bookings waiting for dispatch (Protected: DRIVER or ADMIN)
  // Using hasRole('DRIVER') which checks for authority "ROLE_DRIVER"
  @GetMapping("/available")
  @PreAuthorize("hasRole('DRIVER')")
  public ResponseEntity<List<BookingResponse>> getAvailableBookings(
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(bookingService.getAvailableBookings(userDetails.getUsername()));
  }

  // 4. View history for logged-in user (patient or driver)
  @GetMapping("/my")
  public ResponseEntity<List<BookingResponse>> getMyBookings(@AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(bookingService.getMyBookings(userDetails.getUsername()));
  }

  // 5. Admin fleet overview
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<List<BookingResponse>> getAllBookings() {
    return ResponseEntity.ok(bookingService.getAllBookings());
  }

  // 6. Driver accepts booking (Protected: DRIVER)
  @PutMapping("/{id}/accept")
  @PreAuthorize("hasRole('DRIVER')")
  public ResponseEntity<BookingResponse> acceptBooking(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(bookingService.acceptBooking(id, userDetails.getUsername()));
  }

  // 6a. Driver confirms the booking after price negotiation succeeds
  @PutMapping("/{id}/confirm")
  @PreAuthorize("hasRole('DRIVER')")
  public ResponseEntity<BookingResponse> confirmBooking(
      @PathVariable Long id,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(bookingService.confirmBooking(id, userDetails.getUsername()));
  }

  // 6b. Driver indicates negotiation failed, so the booking can be re-queued safely
  @PutMapping("/{id}/negotiation-failed")
  @PreAuthorize("hasRole('DRIVER')")
  public ResponseEntity<BookingResponse> failNegotiation(
      @PathVariable Long id,
      @RequestParam(required = false) String reason,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(bookingService.failNegotiation(id, userDetails.getUsername(), reason));
  }

  // 6c. Driver cancels before trip start; request is not silently deleted
  @PutMapping("/{id}/cancel-request")
  @PreAuthorize("hasRole('DRIVER')")
  public ResponseEntity<BookingResponse> cancelBookingRequest(
      @PathVariable Long id,
      @RequestParam(required = false) String reason,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(bookingService.cancelBookingRequest(id, userDetails.getUsername(), reason));
  }

  // 6d. System or dispatcher re-queues a booking after failed negotiation or cancellation request
  @PutMapping("/{id}/requeue")
  @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<BookingResponse> requeueBooking(@PathVariable Long id) {
    return ResponseEntity.ok(bookingService.requeueBooking(id));
  }

  // 7. Update status (ASSIGNED -> IN_TRANSIT -> ARRIVED -> COMPLETED / CANCELLED)
  @PutMapping("/{id}/status")
  @PreAuthorize("hasRole('DRIVER')")
  public ResponseEntity<BookingResponse> updateStatus(
      @PathVariable Long id,
      @Valid @RequestBody UpdateBookingStatusRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(bookingService.updateStatus(id, request, userDetails.getUsername()));
  }

  @GetMapping("/driver/active")
  @PreAuthorize("hasRole('DRIVER')")
  public ResponseEntity<BookingResponse> getActiveTrip(
      @AuthenticationPrincipal UserDetails userDetails) {
    return bookingService.getActiveTripForDriver(userDetails.getUsername())
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }
}
