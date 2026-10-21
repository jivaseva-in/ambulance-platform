package com.ambulanceservice.mobile.data.booking.model

import java.time.LocalDateTime

data class BookingResponse(
    val id: Long,
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
    val bookingChannel: BookingChannel,
    val status: BookingStatus,
    val requesterId: Long? = null,
    val requesterName: String? = null,
    val recordedById: Long? = null,
    val recordedByName: String? = null,
    val recordedByRole: String? = null,
    val driverId: Long? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val cancellationReason: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null
)
