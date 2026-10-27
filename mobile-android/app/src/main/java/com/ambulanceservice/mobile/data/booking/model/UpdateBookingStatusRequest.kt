package com.ambulanceservice.mobile.data.booking.model

data class UpdateBookingStatusRequest(
    val status: BookingStatus,
    val cancellationReason: String? = null
)
