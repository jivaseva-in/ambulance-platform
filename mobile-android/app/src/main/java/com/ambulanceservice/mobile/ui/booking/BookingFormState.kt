package com.ambulanceservice.mobile.ui.booking

import com.ambulanceservice.mobile.data.booking.model.AmbulanceType

data class BookingFormState(
    val contactName: String = "",
    val contactPhone: String = "",
    val pickupAddress: String = "",
    val destinationAddress: String = "",
    val ambulanceType: AmbulanceType = AmbulanceType.ADVANCED_LIFE_SUPPORT
)

data class BookingFormErrors(
    val contactName: String? = null,
    val contactPhone: String? = null,
    val pickupAddress: String? = null,
    val destinationAddress: String? = null
) {
    val hasErrors: Boolean
        get() = listOf(contactName, contactPhone, pickupAddress, destinationAddress).any { !it.isNullOrBlank() }

    fun firstMessage(): String? = listOf(contactName, contactPhone, pickupAddress, destinationAddress)
        .firstOrNull { !it.isNullOrBlank() }
}

