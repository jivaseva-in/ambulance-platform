package com.ambulanceservice.mobile.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ambulanceservice.mobile.config.AppEnvironment
import com.ambulanceservice.mobile.data.booking.model.AmbulanceType
import com.ambulanceservice.mobile.data.booking.model.BookingChannel
import com.ambulanceservice.mobile.data.booking.model.BookingResponse
import com.ambulanceservice.mobile.ui.theme.MobileandroidTheme

@Composable
fun BookingScreen(
    uiState: BookingUiState,
    onContactNameChanged: (String) -> Unit,
    onContactPhoneChanged: (String) -> Unit,
    onPickupAddressChanged: (String) -> Unit,
    onDestinationAddressChanged: (String) -> Unit,
    onAmbulanceTypeChanged: (AmbulanceType) -> Unit,
    onSubmit: () -> Unit,
    onTrackBookingIdChanged: (String) -> Unit,
    onTrackBooking: () -> Unit,
    onRefreshLiveLocation: (Long) -> Unit,
    onTabSelected: (BookingTab) -> Unit = {},
    onTabHidden: () -> Unit = {},
    onDismissMessage: () -> Unit = {},
    onDismissTrackingResult: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeaderSection()
                EnvironmentCard()
                IntroCard()

                if (uiState.showErrorMessage && uiState.errorMessage != null) {
                    MessageCard(
                        title = if (uiState.isTracking) "Could not load booking" else "Could not submit booking",
                        message = uiState.errorMessage,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onDismiss = onDismissMessage
                    )
                }

                if (uiState.showSuccessMessage && uiState.successMessage != null && uiState.submittedBooking != null) {
                    SuccessCard(
                        message = uiState.successMessage,
                        booking = uiState.submittedBooking,
                        onDismiss = onDismissMessage
                    )
                }

                if (uiState.showTrackingResult && uiState.trackedBooking != null) {
                    TrackingCard(
                        booking = uiState.trackedBooking,
                        liveLocation = uiState.liveLocation,
                        onRefreshLiveLocation = { onRefreshLiveLocation(uiState.trackedBooking.id) },
                        onDismiss = onDismissTrackingResult
                    )
                }

                BookingTabSelector(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = onTabSelected,
                    onTabHidden = onTabHidden
                )

                if (uiState.selectedTab == BookingTab.BOOKING) {
                    BookingFormCard(
                        uiState = uiState,
                        onContactNameChanged = onContactNameChanged,
                        onContactPhoneChanged = onContactPhoneChanged,
                        onPickupAddressChanged = onPickupAddressChanged,
                        onDestinationAddressChanged = onDestinationAddressChanged,
                        onAmbulanceTypeChanged = onAmbulanceTypeChanged,
                        onSubmit = onSubmit,
                        onCancel = onTabHidden
                    )
                }

                if (uiState.selectedTab == BookingTab.TRACKING) {
                    TrackingLookupCard(
                        uiState = uiState,
                        onTrackBookingIdChanged = onTrackBookingIdChanged,
                        onTrackBooking = onTrackBooking,
                        onCancel = onTabHidden
                    )
                }

                FutureFeaturesCard()
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Request Ambulance",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Emergency booking plus booking lookup and live ambulance tracking for a submitted request.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EnvironmentCard() {
    MessageCard(
        title = "Current environment",
        message = "${AppEnvironment.name} backend\n${AppEnvironment.baseUrl}",
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
}

@Composable
private fun IntroCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "MVP scope",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "This version creates bookings with ONLINE_MOBILE and supports a public lookup by booking ID plus live ambulance location.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun BookingFormCard(
    uiState: BookingUiState,
    onContactNameChanged: (String) -> Unit,
    onContactPhoneChanged: (String) -> Unit,
    onPickupAddressChanged: (String) -> Unit,
    onDestinationAddressChanged: (String) -> Unit,
    onAmbulanceTypeChanged: (AmbulanceType) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Emergency booking form",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = uiState.form.contactName,
                onValueChange = onContactNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contact name") },
                isError = uiState.errors.contactName != null,
                supportingText = {
                    uiState.errors.contactName?.let { Text(it) }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                enabled = !uiState.isSubmitting
            )

            OutlinedTextField(
                value = uiState.form.contactPhone,
                onValueChange = onContactPhoneChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contact phone") },
                isError = uiState.errors.contactPhone != null,
                supportingText = {
                    uiState.errors.contactPhone?.let { Text(it) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                enabled = !uiState.isSubmitting
            )

            OutlinedTextField(
                value = uiState.form.pickupAddress,
                onValueChange = onPickupAddressChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Pickup address") },
                isError = uiState.errors.pickupAddress != null,
                supportingText = {
                    uiState.errors.pickupAddress?.let { Text(it) }
                },
                minLines = 2,
                enabled = !uiState.isSubmitting
            )

            OutlinedTextField(
                value = uiState.form.destinationAddress,
                onValueChange = onDestinationAddressChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Destination hospital / address") },
                isError = uiState.errors.destinationAddress != null,
                supportingText = {
                    uiState.errors.destinationAddress?.let { Text(it) }
                },
                minLines = 2,
                enabled = !uiState.isSubmitting
            )

            Text(
                text = "Ambulance type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            AmbulanceType.values().forEach { type ->
                AmbulanceTypeOption(
                    title = type.displayName,
                    selected = uiState.form.ambulanceType == type,
                    enabled = !uiState.isSubmitting,
                    onClick = { onAmbulanceTypeChanged(type) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSubmitting
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text("Submitting...")
                } else {
                    Text("Request Ambulance")
                }
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun TrackingLookupCard(
    uiState: BookingUiState,
    onTrackBookingIdChanged: (String) -> Unit,
    onTrackBooking: () -> Unit,
    onCancel: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Track a booking by ID",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = uiState.trackBookingId,
                onValueChange = onTrackBookingIdChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Booking ID") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !uiState.isTracking
            )

            Button(
                onClick = onTrackBooking,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isTracking
            ) {
                if (uiState.isTracking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text("Loading...")
                } else {
                    Text("Track status")
                }
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun TrackingCard(
    booking: BookingResponse,
    liveLocation: com.ambulanceservice.mobile.data.booking.model.LocationResponse?,
    onRefreshLiveLocation: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live booking status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Text("×")
                }
            }
            Text("Booking ID: #${booking.id}")
            Text("Status: ${booking.status}")
            Text("Pickup: ${booking.pickupAddress}")
            Text("Destination: ${booking.destinationAddress}")
            Text("Assigned driver: ${booking.driverName ?: "Awaiting assignment"}")

            if (liveLocation != null) {
                Text("Vehicle location: ${liveLocation.driverLatitude}, ${liveLocation.driverLongitude}")
                Text("Last update: ${liveLocation.lastUpdated ?: "Unavailable"}")
                Text("Driver phone: ${liveLocation.driverPhone ?: "Unavailable"}")
            } else {
                Text("Location: Awaiting live GPS update from the driver.")
            }

            Button(
                onClick = onRefreshLiveLocation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh live location")
            }
        }
    }
}

@Composable
private fun SuccessCard(
    message: String,
    booking: BookingResponse,
    onDismiss: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Text("×")
                }
            }
            Text("Booking ID: #${booking.id}")
            Text("Status: ${booking.status}")
            Text("Type: ${booking.ambulanceType.displayName}")
            Text("Route: ${booking.pickupAddress} → ${booking.destinationAddress}")
            Text("Channel: ${booking.bookingChannel.name}")
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    message: String,
    containerColor: Color,
    contentColor: Color,
    onDismiss: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = contentColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Text("×", color = contentColor)
                }
            }
            Text(
                text = message,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookingScreenPreview() {
    MobileandroidTheme {
        BookingScreen(
            uiState = BookingUiState(
                form = BookingFormState(
                    contactName = "John Doe",
                    contactPhone = "+1234567890",
                    pickupAddress = "123 Main Street",
                    destinationAddress = "City General Hospital"
                ),
                successMessage = "Booking #42 submitted successfully.",
                submittedBooking = BookingResponse(
                    id = 42,
                    contactName = "John Doe",
                    contactPhone = "+1234567890",
                    pickupAddress = "123 Main Street",
                    destinationAddress = "City General Hospital",
                    ambulanceType = AmbulanceType.ADVANCED_LIFE_SUPPORT,
                    bookingChannel = BookingChannel.ONLINE_MOBILE,
                    status = "REQUESTED"
                )
            ),
            onContactNameChanged = {},
            onContactPhoneChanged = {},
            onPickupAddressChanged = {},
            onDestinationAddressChanged = {},
            onAmbulanceTypeChanged = {},
            onSubmit = {},
            onTrackBookingIdChanged = {},
            onTrackBooking = {},
            onRefreshLiveLocation = {}
        )
    }
}

@Composable
private fun BookingTabSelector(
    selectedTab: BookingTab?,
    onTabSelected: (BookingTab) -> Unit,
    onTabHidden: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { onTabSelected(BookingTab.BOOKING) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Book")
        }

        Button(
            onClick = { onTabSelected(BookingTab.TRACKING) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Track")
        }
    }
}

@Composable
private fun FutureFeaturesCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Next upgrades",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text("• GPS pickup detection")
            Text("• Booking tracking by ID")
            Text("• Login / registration")
            Text("• Driver and dispatcher workflows")
        }
    }
}

@Composable
private fun AmbulanceTypeOption(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column {
            Text(text = title, fontWeight = FontWeight.Medium)
            Text(
                text = when (title) {
                    "Basic Life Support" -> "Standard emergency care for most requests."
                    "Advanced Life Support" -> "Paramedic support and critical-response equipment."
                    "ICU / Ventilator" -> "High-acuity transfer and respiratory support."
                    else -> "Non-emergency or monitored transport."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.dp)
        )
        Box(
            modifier = Modifier
                .size(width = 2.dp, height = 36.dp)
                .background(borderColor, RoundedCornerShape(999.dp))
        )
    }
}
