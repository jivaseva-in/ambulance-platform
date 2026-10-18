package com.ambulanceservice.mobile.data.booking.model

data class CreateBookingRequest(
    val contactName: String,
    val contactPhone: String,
    val pickupAddress: String,
    val pickupLatitude: Double? = null,
    val pickupLongitude: Double? = null,
    val destinationAddress: String,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    val patientCondition: String? = null,
    val ambulanceType: AmbulanceType,
    val bookingChannel: BookingChannel
)

