package com.ambulanceservice.user.controller;

import com.ambulanceservice.user.dto.DriverAvailabilityRequest;
import com.ambulanceservice.user.dto.DriverAvailabilityResponse;
import com.ambulanceservice.user.dto.DriverLocationUpdateRequest;
import com.ambulanceservice.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

  private final UserService userService;

  @GetMapping("/me/availability")
  @PreAuthorize("hasRole('DRIVER')")
  public ResponseEntity<DriverAvailabilityResponse> getMyAvailability(
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(userService.getDriverAvailability(userDetails.getUsername()));
  }

  @PutMapping("/me/availability")
  @PreAuthorize("hasRole('DRIVER')")
  public ResponseEntity<DriverAvailabilityResponse> updateMyAvailability(
      @Valid @RequestBody DriverAvailabilityRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(userService.updateDriverAvailability(userDetails.getUsername(), request));
  }

  @PutMapping("/me/location")
  @PreAuthorize("hasRole('DRIVER')")
  public ResponseEntity<DriverAvailabilityResponse> updateMyLocation(
      @Valid @RequestBody DriverLocationUpdateRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(userService.updateDriverLocation(userDetails.getUsername(), request));
  }
}
