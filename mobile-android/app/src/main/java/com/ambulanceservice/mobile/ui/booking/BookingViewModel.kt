package com.ambulanceservice.mobile.ui.booking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ambulanceservice.mobile.data.booking.model.BookingChannel
import com.ambulanceservice.mobile.data.booking.model.CreateBookingRequest
import com.ambulanceservice.mobile.data.booking.repository.BookingLookupResult
import com.ambulanceservice.mobile.data.booking.repository.BookingRepository
import com.ambulanceservice.mobile.data.booking.repository.BookingSubmissionResult
import com.ambulanceservice.mobile.data.booking.repository.BookingTrackingRepository
import com.ambulanceservice.mobile.data.booking.repository.TrackingResult
import kotlinx.coroutines.launch

class BookingViewModel(
    private val bookingRepository: BookingRepository,
    private val trackingRepository: BookingTrackingRepository
) : ViewModel() {

    var uiState by mutableStateOf(BookingUiState())
        private set

    fun onContactNameChanged(value: String) {
        updateForm(uiState.form.copy(contactName = value))
    }

    fun onContactPhoneChanged(value: String) {
        updateForm(uiState.form.copy(contactPhone = value))
    }

    fun onPickupCoordinatesChanged(latitude: Double, longitude: Double) {
        updateForm(uiState.form.copy(
            pickupLatitude = latitude,
            pickupLongitude = longitude
        ))
    }

    fun onDestinationCoordinatesChanged(latitude: Double, longitude: Double) {
        updateForm(uiState.form.copy(
            destinationLatitude = latitude,
            destinationLongitude = longitude
        ))
    }

    fun onPickupAddressChanged(value: String) {
        updateForm(uiState.form.copy(
            pickupAddress = value,
            pickupLatitude = if (value.isBlank()) null else uiState.form.pickupLatitude,
            pickupLongitude = if (value.isBlank()) null else uiState.form.pickupLongitude
        ))
    }

    fun onDestinationAddressChanged(value: String) {
        updateForm(uiState.form.copy(
            destinationAddress = value,
            destinationLatitude = if (value.isBlank()) null else uiState.form.destinationLatitude,
            destinationLongitude = if (value.isBlank()) null else uiState.form.destinationLongitude
        ))
    }

    fun onAmbulanceTypeChanged(value: com.ambulanceservice.mobile.data.booking.model.AmbulanceType) {
        updateForm(uiState.form.copy(ambulanceType = value))
    }

    fun onTrackBookingIdChanged(value: String) {
        uiState = uiState.copy(trackBookingId = value)
    }

    fun onTrackPhoneNumberChanged(value: String) {
        uiState = uiState.copy(trackPhoneNumber = value)
    }

    fun onLookupModeChanged(mode: BookingLookupMode) {
        uiState = uiState.copy(lookupMode = mode)
    }

    fun onTabSelected(tab: BookingTab) {
        uiState = uiState.copy(selectedTab = tab)
    }

    fun onTabHidden() {
        uiState = uiState.copy(selectedTab = null)
    }

    fun dismissMessage() {
        uiState = uiState.copy(
            errorMessage = null,
            showErrorMessage = false,
            successMessage = null,
            showSuccessMessage = false
        )
    }

    fun dismissTrackingResult() {
        uiState = uiState.copy(
            trackedBooking = null,
            showTrackingResult = false,
            liveLocation = null
        )
    }

    fun submitBooking() {
        val validation = BookingFormValidator.validate(uiState.form)
        if (validation.hasErrors) {
            uiState = uiState.copy(
                errors = validation,
                errorMessage = validation.firstMessage(),
                showErrorMessage = true,
                successMessage = null,
                showSuccessMessage = false
            )
            return
        }

        val currentForm = uiState.form
        uiState = uiState.copy(
            isSubmitting = true,
            errors = BookingFormErrors(),
            errorMessage = null,
            showErrorMessage = false,
            successMessage = null,
            showSuccessMessage = false
        )

        viewModelScope.launch {
            val request = CreateBookingRequest(
                contactName = currentForm.contactName.trim(),
                contactPhone = currentForm.contactPhone.trim(),
                pickupAddress = currentForm.pickupAddress.trim(),
                pickupLatitude = currentForm.pickupLatitude,
                pickupLongitude = currentForm.pickupLongitude,
                destinationAddress = currentForm.destinationAddress.trim(),
                destinationLatitude = currentForm.destinationLatitude,
                destinationLongitude = currentForm.destinationLongitude,
                ambulanceType = currentForm.ambulanceType,
                bookingChannel = BookingChannel.ONLINE_MOBILE
            )

            when (val result = bookingRepository.createBooking(request)) {
                is BookingSubmissionResult.Success -> {
                    val booking = result.booking
                    uiState = BookingUiState(
                        form = BookingFormState(),
                        successMessage = "Booking #${booking.id} submitted successfully.",
                        showSuccessMessage = true,
                        submittedBooking = booking,
                        trackedBooking = booking,
                        trackBookingId = booking.id.toString()
                    )
                }

                is BookingSubmissionResult.Error -> {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage = result.message,
                        showErrorMessage = true,
                        successMessage = null,
                        showSuccessMessage = false
                    )
                }
            }
        }
    }

    fun trackBookingById() {
        val bookingIdText = uiState.trackBookingId.trim()
        if (bookingIdText.isEmpty()) {
            uiState = uiState.copy(
                errorMessage = "Enter a booking ID to track.",
                showErrorMessage = true,
                successMessage = null,
                showSuccessMessage = false
            )
            return
        }

        val bookingId = bookingIdText.toLongOrNull()
        if (bookingId == null) {
            uiState = uiState.copy(
                errorMessage = "Booking ID must be a number.",
                showErrorMessage = true,
                successMessage = null,
                showSuccessMessage = false
            )
            return
        }

        uiState = uiState.copy(
            isTracking = true,
            errorMessage = null,
            showErrorMessage = false,
            successMessage = null,
            showSuccessMessage = false
        )

        viewModelScope.launch {
            when (val result = bookingRepository.getBookingById(bookingId)) {
                is BookingLookupResult.Success -> {
                    val trackedBooking = result.booking
                    uiState = uiState.copy(
                        isTracking = false,
                        trackedBooking = trackedBooking,
                        showTrackingResult = true,
                        successMessage = "Booking #${trackedBooking.id} loaded successfully.",
                        showSuccessMessage = true,
                        errorMessage = null,
                        showErrorMessage = false
                    )
                    fetchLiveLocation(bookingId)
                }

                is BookingLookupResult.Error -> {
                    uiState = uiState.copy(
                        isTracking = false,
                        errorMessage = result.message,
                        showErrorMessage = true,
                        trackedBooking = null,
                        showTrackingResult = false,
                        liveLocation = null
                    )
                }
            }
        }
    }

    fun trackBookingByPhone() {
        val phone = uiState.trackPhoneNumber.trim()
        if (phone.isEmpty()) {
            uiState = uiState.copy(
                errorMessage = "Enter a phone number to find the latest booking.",
                showErrorMessage = true,
                successMessage = null,
                showSuccessMessage = false
            )
            return
        }

        val normalizedPhone = phone.replace(" ".toRegex(), "")
        if (normalizedPhone.isEmpty()) {
            uiState = uiState.copy(
                errorMessage = "Phone number cannot be empty.",
                showErrorMessage = true,
                successMessage = null,
                showSuccessMessage = false
            )
            return
        }

        uiState = uiState.copy(
            isTracking = true,
            errorMessage = null,
            showErrorMessage = false,
            successMessage = null,
            showSuccessMessage = false
        )

        viewModelScope.launch {
            when (val result = trackingRepository.getBookingByPhone(normalizedPhone)) {
                is BookingLookupResult.Success -> {
                    val trackedBooking = result.booking
                    uiState = uiState.copy(
                        isTracking = false,
                        trackedBooking = trackedBooking,
                        showTrackingResult = true,
                        successMessage = "Latest booking for ${trackedBooking.contactPhone} loaded successfully.",
                        showSuccessMessage = true,
                        errorMessage = null,
                        showErrorMessage = false
                    )
                    fetchLiveLocation(trackedBooking.id)
                }

                is BookingLookupResult.Error -> {
                    uiState = uiState.copy(
                        isTracking = false,
                        errorMessage = result.message,
                        showErrorMessage = true,
                        trackedBooking = null,
                        showTrackingResult = false,
                        liveLocation = null
                    )
                }
            }
        }
    }

    fun trackBooking() {
        when (uiState.lookupMode) {
            BookingLookupMode.ID -> trackBookingById()
            BookingLookupMode.PHONE -> trackBookingByPhone()
        }
    }

    fun fetchLiveLocation(bookingId: Long) {
        viewModelScope.launch {
            when (val result = trackingRepository.getLiveLocation(bookingId)) {
                is TrackingResult.Success -> {
                    uiState = uiState.copy(
                        liveLocation = result.location,
                        errorMessage = null,
                        showErrorMessage = false
                    )
                }

                is TrackingResult.Error -> {
                    uiState = uiState.copy(
                        errorMessage = result.message,
                        showErrorMessage = true
                    )
                }
            }
        }
    }

    private fun updateForm(newForm: BookingFormState) {
        uiState = uiState.copy(
            form = newForm,
            errors = BookingFormErrors(),
            errorMessage = null,
            showErrorMessage = false,
            successMessage = null,
            showSuccessMessage = false
        )
    }
}

class BookingViewModelFactory(
    private val bookingRepository: BookingRepository,
    private val trackingRepository: BookingTrackingRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookingViewModel::class.java)) {
            return BookingViewModel(bookingRepository, trackingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
