package com.ambulanceservice.mobile

import com.ambulanceservice.mobile.data.booking.model.AmbulanceType
import com.ambulanceservice.mobile.ui.booking.BookingFormState
import com.ambulanceservice.mobile.ui.booking.BookingFormValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun invalidBookingForm_returnsErrors() {
        val errors = BookingFormValidator.validate(
            BookingFormState(
                contactName = "J",
                contactPhone = "123",
                pickupAddress = "",
                destinationAddress = "",
                ambulanceType = AmbulanceType.BASIC_LIFE_SUPPORT
            )
        )

        assertTrue(errors.hasErrors)
        assertFalse(errors.contactName.isNullOrBlank())
        assertFalse(errors.contactPhone.isNullOrBlank())
        assertFalse(errors.pickupAddress.isNullOrBlank())
        assertFalse(errors.destinationAddress.isNullOrBlank())
    }

    @Test
    fun validBookingForm_returnsNoErrors() {
        val errors = BookingFormValidator.validate(
            BookingFormState(
                contactName = "John Doe",
                contactPhone = "+1234567890",
                pickupAddress = "123 Main Street",
                destinationAddress = "City General Hospital",
                ambulanceType = AmbulanceType.ADVANCED_LIFE_SUPPORT
            )
        )

        assertFalse(errors.hasErrors)
    }
}