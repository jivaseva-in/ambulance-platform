package com.ambulanceservice.tracking.service;

import com.ambulanceservice.booking.entity.Booking;
import com.ambulanceservice.booking.repository.BookingRepository;
import com.ambulanceservice.tracking.dto.LocationResponse;
import com.ambulanceservice.tracking.dto.LocationUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TrackingService {

  private final BookingRepository bookingRepository;

  @Transactional
  public LocationResponse updateDriverLocation(Long bookingId, LocationUpdateRequest request, String driverEmail) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));

    if (booking.getAssignedDriver() == null || !booking.getAssignedDriver().getEmail().equalsIgnoreCase(driverEmail)) {
      throw new IllegalArgumentException("Only the assigned driver can update telemetry for this booking");
    }

    booking.setDriverLatitude(request.latitude());
    booking.setDriverLongitude(request.longitude());
    booking.setLastLocationUpdate(LocalDateTime.now());
    bookingRepository.save(booking);

    return mapToResponse(booking);
  }

  @Transactional(readOnly = true)
  public LocationResponse getLiveLocation(Long bookingId) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));

    return mapToResponse(booking);
  }

  private LocationResponse mapToResponse(Booking booking) {
    String driverName = booking.getAssignedDriver() != null ? booking.getAssignedDriver().getFullName() : "Unassigned";
    String driverPhone = booking.getAssignedDriver() != null ? booking.getAssignedDriver().getPhone() : "N/A";

    return new LocationResponse(
        booking.getId(),
        booking.getStatus().name(),
        booking.getDriverLatitude(),
        booking.getDriverLongitude(),
        booking.getLastLocationUpdate(),
        driverName,
        driverPhone
    );
  }
}
