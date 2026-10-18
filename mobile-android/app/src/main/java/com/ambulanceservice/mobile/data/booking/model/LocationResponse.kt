package com.ambulanceservice.mobile.data.booking.model

data class LocationResponse(
    val bookingId: Long,
    val status: String? = null,
    val driverLatitude: Double? = null,
    val driverLongitude: Double? = null,
    val lastUpdated: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null
)

