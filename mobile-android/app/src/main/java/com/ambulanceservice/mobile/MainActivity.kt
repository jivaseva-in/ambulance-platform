package com.ambulanceservice.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.saveable.rememberSaveable
import com.ambulanceservice.mobile.di.AppContainer
import com.ambulanceservice.mobile.ui.auth.AuthScreen
import com.ambulanceservice.mobile.ui.booking.BookingScreen
import com.ambulanceservice.mobile.ui.booking.BookingViewModel
import com.ambulanceservice.mobile.ui.booking.BookingViewModelFactory
import com.ambulanceservice.mobile.ui.driver.DriverPortalScreen
import com.ambulanceservice.mobile.data.auth.model.AuthResponse
import com.ambulanceservice.mobile.ui.theme.MobileandroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileandroidTheme {
                var showDriverAuth by rememberSaveable { mutableStateOf(false) }
                var showDriverPortal by rememberSaveable { mutableStateOf(false) }
                var driverToken by rememberSaveable { mutableStateOf<String?>(null) }

                if (driverToken != null && showDriverPortal) {
                    DriverPortalScreen(
                        authToken = driverToken!!,
                        onBackToBooking = { showDriverPortal = false },
                        onLogout = {
                            driverToken = null
                            showDriverPortal = false
                            showDriverAuth = false
                        }
                    )
                } else if (showDriverAuth) {
                    AuthScreen(
                        onAuthenticated = { auth ->
                            driverToken = auth.token
                            showDriverAuth = false
                            showDriverPortal = true
                        },
                        onCancel = { showDriverAuth = false }
                    )
                } else {
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
                        onDismissTrackingResult = bookingViewModel::dismissTrackingResult,
                        onDriverAccessClick = {
                            if (driverToken != null) {
                                showDriverPortal = true
                            } else {
                                showDriverAuth = true
                            }
                        }
                    )
                }
            }
        }
    }
}
