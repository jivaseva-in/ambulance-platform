package com.ambulanceservice.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ambulanceservice.mobile.di.AppContainer
import com.ambulanceservice.mobile.ui.booking.BookingScreen
import com.ambulanceservice.mobile.ui.booking.BookingViewModel
import com.ambulanceservice.mobile.ui.booking.BookingViewModelFactory
import com.ambulanceservice.mobile.ui.theme.MobileandroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileandroidTheme {
                val bookingViewModel: BookingViewModel = viewModel(
                    factory = BookingViewModelFactory(
                        AppContainer.bookingRepository,
                        AppContainer.bookingTrackingRepository
                    )
                )

                BookingScreen(
                    uiState = bookingViewModel.uiState,
                    onContactNameChanged = bookingViewModel::onContactNameChanged,
                    onContactPhoneChanged = bookingViewModel::onContactPhoneChanged,
                    onPickupAddressChanged = bookingViewModel::onPickupAddressChanged,
                    onDestinationAddressChanged = bookingViewModel::onDestinationAddressChanged,
                    onPickupCoordinatesChanged = bookingViewModel::onPickupCoordinatesChanged,
                    onDestinationCoordinatesChanged = bookingViewModel::onDestinationCoordinatesChanged,
                    onAmbulanceTypeChanged = bookingViewModel::onAmbulanceTypeChanged,
                    onSubmit = bookingViewModel::submitBooking,
                    onTrackBookingIdChanged = bookingViewModel::onTrackBookingIdChanged,
                    onTrackPhoneNumberChanged = bookingViewModel::onTrackPhoneNumberChanged,
                    onLookupModeChanged = bookingViewModel::onLookupModeChanged,
                    onTrackBooking = bookingViewModel::trackBooking,
                    onRefreshLiveLocation = bookingViewModel::fetchLiveLocation,
                    onTabSelected = bookingViewModel::onTabSelected,
                    onTabHidden = bookingViewModel::onTabHidden,
                    onDismissMessage = bookingViewModel::dismissMessage,
                    onDismissTrackingResult = bookingViewModel::dismissTrackingResult
                )
            }
        }
    }
}
