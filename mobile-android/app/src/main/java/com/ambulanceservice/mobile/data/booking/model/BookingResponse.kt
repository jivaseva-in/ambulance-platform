package com.ambulanceservice.mobile.data.booking.model

data class BookingResponse(
    val id: Long,
    val contactName: String,
    val contactPhone: String,
    val pickupAddress: String,
    val destinationAddress: String,
    val ambulanceType: AmbulanceType,
    val bookingChannel: BookingChannel,
    val status: String,
    val requesterName: String? = null,
    val recordedByName: String? = null,
    val driverName: String? = null
)

