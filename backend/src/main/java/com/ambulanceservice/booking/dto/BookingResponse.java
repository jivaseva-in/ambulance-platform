package com.ambulanceservice.booking.dto;

import com.ambulanceservice.booking.entity.*;

import java.time.LocalDateTime;

public record BookingResponse(
    Long id,
    String contactName,
    String contactPhone,
    String pickupAddress,
    Double pickupLatitude,
    Double pickupLongitude,
    String destinationAddress,
    Double destinationLatitude,
    Double destinationLongitude,
    String patientCondition,
    AmbulanceType ambulanceType,
    BookingChannel bookingChannel,
    BookingStatus status,
    Long requesterId,
    String requesterName,
    Long driverId,
    String driverName,
    String driverPhone,
    String cancellationReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime completedAt
) {
  public static BookingResponse fromEntity(Booking b) {
    return new BookingResponse(
        b.getId(),
        b.getContactName(),
        b.getContactPhone(),
        b.getPickupAddress(),
        b.getPickupLatitude(),
        b.getPickupLongitude(),
        b.getDestinationAddress(),
        b.getDestinationLatitude(),
        b.getDestinationLongitude(),
        b.getPatientCondition(),
        b.getAmbulanceType(),
        b.getBookingChannel(),
        b.getStatus(),
        b.getRequester() != null ? b.getRequester().getId() : null,
        b.getRequester() != null ? b.getRequester().getFullName() : "Anonymous Emergency Guest",
        b.getAssignedDriver() != null ? b.getAssignedDriver().getId() : null,
        b.getAssignedDriver() != null ? b.getAssignedDriver().getFullName() : null,
        b.getAssignedDriver() != null ? b.getAssignedDriver().getPhone() : null,
        b.getCancellationReason(),
        b.getCreatedAt(),
        b.getUpdatedAt(),
        b.getCompletedAt()
    );
  }
}
