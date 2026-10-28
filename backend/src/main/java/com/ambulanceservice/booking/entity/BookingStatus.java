package com.ambulanceservice.booking.entity;

public enum BookingStatus {
  REQUESTED,           // Created by user or dispatcher; waiting for a driver
  DRIVER_HELD,         // Driver accepted the trip and is negotiating; still provisional
  NEGOTIATION_FAILED,  // Price or coordination failed; request must be re-opened
  REASSIGNMENT_PENDING, // Booking was released and is being re-queued for another driver
  CONFIRMED,           // Driver and caller agreed on the trip details/fare
  ASSIGNED,            // Booking is officially assigned to a driver
  IN_TRANSIT,          // Driver is en route to pickup
  ARRIVED,             // Driver arrived at the patient location
  COMPLETED,           // Trip finished successfully
  CANCEL_REQUESTED,    // Driver or caller requested cancellation; reassignment should follow
  CANCELLED,           // Final cancellation state
  EXPIRED              // Held reservation timed out without confirmation
}
