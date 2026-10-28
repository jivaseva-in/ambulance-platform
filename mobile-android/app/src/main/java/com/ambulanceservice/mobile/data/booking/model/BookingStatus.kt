package com.ambulanceservice.mobile.data.booking.model

enum class BookingStatus {
    REQUESTED,
    DRIVER_HELD,
    NEGOTIATION_FAILED,
    REASSIGNMENT_PENDING,
    CONFIRMED,
    ASSIGNED,
    IN_TRANSIT,
    ARRIVED,
    COMPLETED,
    CANCEL_REQUESTED,
    CANCELLED,
    EXPIRED
}
