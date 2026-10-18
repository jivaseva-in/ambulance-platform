package com.ambulanceservice.mobile.ui.booking

import com.ambulanceservice.mobile.data.booking.model.BookingResponse
import com.ambulanceservice.mobile.data.booking.model.LocationResponse

enum class BookingTab {
    BOOKING,
    TRACKING
}

data class BookingUiState(
    val form: BookingFormState = BookingFormState(),
    val errors: BookingFormErrors = BookingFormErrors(),
    val isSubmitting: Boolean = false,
    val isTracking: Boolean = false,
    val selectedTab: BookingTab? = null,
    val trackBookingId: String = "",
    val errorMessage: String? = null,
    val showErrorMessage: Boolean = false,
    val successMessage: String? = null,
    val showSuccessMessage: Boolean = false,
    val submittedBooking: BookingResponse? = null,
    val trackedBooking: BookingResponse? = null,
    val showTrackingResult: Boolean = false,
    val liveLocation: LocationResponse? = null
)
