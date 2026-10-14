package com.ambulanceservice.booking.entity;

public enum BookingStatus {
  REQUESTED,   // Placed by user or dispatcher; waiting for driver
  ASSIGNED,    // Driver accepted the booking
  IN_TRANSIT,  // Driver is en route to pickup
  ARRIVED,     // Driver arrived at patient location
  COMPLETED,   // Patient safely reached destination hospital
  CANCELLED    // Cancelled by patient, dispatcher, or driver
}
