package com.ambulanceservice.mobile.ui.booking

object BookingFormValidator {
    fun validate(form: BookingFormState): BookingFormErrors {
        val trimmedName = form.contactName.trim()
        val trimmedPhone = form.contactPhone.trim()
        val trimmedPickup = form.pickupAddress.trim()
        val trimmedDestination = form.destinationAddress.trim()

        return BookingFormErrors(
            contactName = when {
                trimmedName.isEmpty() -> "Contact name is required."
                trimmedName.length < 2 -> "Contact name must be at least 2 characters."
                else -> null
            },
            contactPhone = when {
                trimmedPhone.isEmpty() -> "Contact phone is required."
                trimmedPhone.length < 8 -> "Contact phone must be at least 8 characters."
                else -> null
            },
            pickupAddress = if (trimmedPickup.isEmpty()) "Pickup address is required." else null,
            destinationAddress = if (trimmedDestination.isEmpty()) "Destination address is required." else null
        )
    }
}

