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
    return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request, email));
  }

  // 2. Track booking details by ID (PUBLIC: Allows patient/guest to monitor their assigned ambulance)
  @GetMapping("/{id}")
  public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
    return ResponseEntity.ok(bookingService.getBookingById(id));
  }

  // 3. Driver feeds: Get unassigned bookings waiting for dispatch (Protected: DRIVER or ADMIN)
  // Using hasRole('DRIVER') which checks for authority "ROLE_DRIVER"
  @GetMapping("/available")
  @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
  public ResponseEntity<List<BookingResponse>> getAvailableBookings() {
    return ResponseEntity.ok(bookingService.getAvailableBookings());
  }

  // 4. View history for logged-in user (patient or driver)
  @GetMapping("/my")
  public ResponseEntity<List<BookingResponse>> getMyBookings(@AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(bookingService.getMyBookings(userDetails.getUsername()));
  }

  // 5. Admin fleet overview
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
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

  // 7. Update status (ASSIGNED -> IN_TRANSIT -> ARRIVED -> COMPLETED / CANCELLED)
  @PutMapping("/{id}/status")
  @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
  public ResponseEntity<BookingResponse> updateStatus(
      @PathVariable Long id,
      @Valid @RequestBody UpdateBookingStatusRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(bookingService.updateStatus(id, request, userDetails.getUsername()));
  }

  @GetMapping("/driver/active")
  @PreAuthorize("hasRole('DRIVER') or hasRole('ADMIN')")
  public ResponseEntity<BookingResponse> getActiveTrip(
      @AuthenticationPrincipal UserDetails userDetails) {
    return bookingService.getActiveTripForDriver(userDetails.getUsername())
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }
}
