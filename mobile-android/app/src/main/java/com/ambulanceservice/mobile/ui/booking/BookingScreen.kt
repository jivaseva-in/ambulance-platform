package com.ambulanceservice.mobile.ui.booking

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ambulanceservice.mobile.config.AppEnvironment
import com.ambulanceservice.mobile.data.booking.model.AmbulanceType
import com.ambulanceservice.mobile.data.booking.model.BookingChannel
import com.ambulanceservice.mobile.data.booking.model.BookingResponse
import com.ambulanceservice.mobile.data.booking.model.BookingStatus
import com.ambulanceservice.mobile.ui.theme.MobileandroidTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.style.TextAlign

private data class MapCoordinate(
    val latitude: Double,
    val longitude: Double
)

@Composable
fun BookingScreen(
    uiState: BookingUiState,
    onContactNameChanged: (String) -> Unit,
    onContactPhoneChanged: (String) -> Unit,
    onPickupAddressChanged: (String) -> Unit,
    onDestinationAddressChanged: (String) -> Unit,
    onPickupCoordinatesChanged: (Double, Double) -> Unit,
    onDestinationCoordinatesChanged: (Double, Double) -> Unit,
    onAmbulanceTypeChanged: (AmbulanceType) -> Unit,
    onSubmit: () -> Unit,
    onTrackBookingIdChanged: (String) -> Unit,
    onTrackPhoneNumberChanged: (String) -> Unit,
    onLookupModeChanged: (BookingLookupMode) -> Unit,
    onTrackBooking: () -> Unit,
    onRefreshLiveLocation: (Long) -> Unit,
    onTabSelected: (BookingTab) -> Unit = {},
    onTabHidden: () -> Unit = {},
    onDismissMessage: () -> Unit = {},
    onDismissTrackingResult: () -> Unit = {},
    onDriverAccessClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val emergencyPhone = AppEnvironment.emergencyPhoneNumber

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EmergencyCallCard(
                    phoneNumber = emergencyPhone,
                    onCallClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$emergencyPhone"))
                        context.startActivity(intent)
                    }
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Request Ambulance",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Emergency booking plus booking lookup and live ambulance tracking for a submitted request.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

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
                                onPickupCoordinatesChanged = onPickupCoordinatesChanged,
                                onDestinationCoordinatesChanged = onDestinationCoordinatesChanged,
                                onAmbulanceTypeChanged = onAmbulanceTypeChanged,
                                onSubmit = onSubmit,
                                onCancel = onTabHidden
                            )
                        }

                        if (uiState.selectedTab == BookingTab.TRACKING) {
                            TrackingLookupCard(
                                uiState = uiState,
                                onTrackBookingIdChanged = onTrackBookingIdChanged,
                                onTrackPhoneNumberChanged = onTrackPhoneNumberChanged,
                                onLookupModeChanged = onLookupModeChanged,
                                onTrackBooking = onTrackBooking,
                                onCancel = onTabHidden
                            )
                        }
                    }
                }

                DriverAccessCard(onDriverAccessClick = onDriverAccessClick)

                if (uiState.showTrackingResult && uiState.trackedBooking != null) {
                    TrackingCard(
                        booking = uiState.trackedBooking,
                        liveLocation = uiState.liveLocation,
                        onRefreshLiveLocation = { onRefreshLiveLocation(uiState.trackedBooking.id) },
                        onDismiss = onDismissTrackingResult
                    )
                }

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
private fun EmergencyCallCard(
    phoneNumber: String,
    onCallClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Emergency",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onError
            )
            Button(
                onClick = onCallClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "Call $phoneNumber",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
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
private fun BookingFormCard(
    uiState: BookingUiState,
    onContactNameChanged: (String) -> Unit,
    onContactPhoneChanged: (String) -> Unit,
    onPickupAddressChanged: (String) -> Unit,
    onDestinationAddressChanged: (String) -> Unit,
    onPickupCoordinatesChanged: (Double, Double) -> Unit,
    onDestinationCoordinatesChanged: (Double, Double) -> Unit,
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

            PickupLocationSelector(
                currentAddress = uiState.form.pickupAddress,
                currentLatitude = uiState.form.pickupLatitude,
                currentLongitude = uiState.form.pickupLongitude,
                onAddressChanged = onPickupAddressChanged,
                onCoordinatesChanged = { lat, lng ->
                    onPickupCoordinatesChanged(lat, lng)
                },
                isSubmitting = uiState.isSubmitting
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
    onTrackPhoneNumberChanged: (String) -> Unit,
    onLookupModeChanged: (BookingLookupMode) -> Unit,
    onTrackBooking: () -> Unit,
    onCancel: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Track a booking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onLookupModeChanged(BookingLookupMode.ID) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isTracking,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("By ID")
                }

                Button(
                    onClick = { onLookupModeChanged(BookingLookupMode.PHONE) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isTracking,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("By phone")
                }
            }

            if (uiState.lookupMode == BookingLookupMode.ID) {
                OutlinedTextField(
                    value = uiState.trackBookingId,
                    onValueChange = onTrackBookingIdChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Booking ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !uiState.isTracking
                )
            } else {
                OutlinedTextField(
                    value = uiState.trackPhoneNumber,
                    onValueChange = onTrackPhoneNumberChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Phone number") },
                    placeholder = { Text("+254712345678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    enabled = !uiState.isTracking
                )
            }

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
                    Text(if (uiState.lookupMode == BookingLookupMode.ID) "Track status" else "Find latest booking")
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
private fun ClickablePhoneText(phone: String?, label: String = "Call") {
    val context = LocalContext.current
    val sanitizedPhone = phone?.replace(Regex("[^+\\d]"), "")?.trim()

    Text(
        text = if (sanitizedPhone.isNullOrBlank()) "$label: unavailable" else "$label: $sanitizedPhone",
        style = MaterialTheme.typography.bodyMedium,
        color = if (sanitizedPhone.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable(enabled = !sanitizedPhone.isNullOrBlank()) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitizedPhone"))
            context.startActivity(dialIntent)
        }
    )
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Live booking status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Booking #${booking.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Text("×")
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                tonalElevation = 0.dp
            ) {
                Text(
                    text = booking.status.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Pickup: ${booking.pickupAddress}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Destination: ${booking.destinationAddress}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (booking.driverName != null) {
                Text(
                    text = "Assigned driver: ${booking.driverName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Assigned driver: Awaiting assignment",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (liveLocation != null || !booking.driverName.isNullOrBlank()) {
                ClickablePhoneText(
                    phone = booking.driverPhone ?: liveLocation?.driverPhone,
                    label = "Driver phone"
                )
            }

            if (liveLocation != null) {
                Text(
                    text = "Live GPS updated: ${liveLocation.lastUpdated ?: "Recently"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onRefreshLiveLocation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh live status")
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
                    status = BookingStatus.REQUESTED
                )
            ),
            onContactNameChanged = {},
            onContactPhoneChanged = {},
            onPickupAddressChanged = {},
            onDestinationAddressChanged = {},
            onPickupCoordinatesChanged = { _, _ -> },
            onDestinationCoordinatesChanged = { _, _ -> },
            onAmbulanceTypeChanged = {},
            onSubmit = {},
            onTrackBookingIdChanged = {},
            onTrackPhoneNumberChanged = {},
            onLookupModeChanged = {},
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickupLocationSelector(
    currentAddress: String,
    currentLatitude: Double?,
    currentLongitude: Double?,
    onAddressChanged: (String) -> Unit,
    onCoordinatesChanged: (Double, Double) -> Unit,
    isSubmitting: Boolean
) {
    var showMapPicker by remember { mutableStateOf(false) }
    var selectedCoordinate by remember {
        mutableStateOf(
            if (currentLatitude != null && currentLongitude != null) {
                MapCoordinate(currentLatitude, currentLongitude)
            } else {
                MapCoordinate(6.5244, 3.3792)
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pickup location",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = { showMapPicker = true },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Select on map")
            }
        }

        OutlinedTextField(
            value = currentAddress,
            onValueChange = onAddressChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Pickup address") },
            minLines = 2,
            enabled = !isSubmitting
        )

        if (currentLatitude != null && currentLongitude != null) {
            Text(
                text = "Selected: ${"%.5f".format(currentLatitude)}, ${"%.5f".format(currentLongitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showMapPicker) {
            ModalBottomSheet(
                onDismissRequest = { showMapPicker = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Choose pickup point",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MapLibre / OSM preview")
                            Text(
                                text = "Local testing mode: no Google API key required",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Selected coordinate: ${"%.5f".format(selectedCoordinate.latitude)}, ${"%.5f".format(selectedCoordinate.longitude)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedCoordinate = MapCoordinate(6.5244, 3.3792)
                                onCoordinatesChanged(6.5244, 3.3792)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset")
                        }

                        Button(
                            onClick = { showMapPicker = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DriverAccessCard(onDriverAccessClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Driver access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Open the driver portal to view trip status and update your availability.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onDriverAccessClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open driver portal")
            }
        }
    }
}
