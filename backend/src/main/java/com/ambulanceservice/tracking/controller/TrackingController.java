package com.ambulanceservice.tracking.controller;

import com.ambulanceservice.tracking.dto.LocationResponse;
import com.ambulanceservice.tracking.dto.LocationUpdateRequest;
import com.ambulanceservice.tracking.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/location")
@RequiredArgsConstructor
public class TrackingController {

  private final TrackingService trackingService;

  // Driver pushes GPS coordinate stream (Protected: DRIVER)
  @PutMapping
  @PreAuthorize("hasRole('DRIVER')")
  public ResponseEntity<LocationResponse> updateLocation(
      @PathVariable Long bookingId,
      @Valid @RequestBody LocationUpdateRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(trackingService.updateDriverLocation(bookingId, request, userDetails.getUsername()));
  }

  // Public live tracking endpoint: Requester/Guest monitors incoming ambulance location
  @GetMapping
  public ResponseEntity<LocationResponse> getLiveLocation(@PathVariable Long bookingId) {
    return ResponseEntity.ok(trackingService.getLiveLocation(bookingId));
  }
}
