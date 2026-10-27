package com.ambulanceservice.mobile.ui.driver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ambulanceservice.mobile.data.booking.model.BookingResponse
import com.ambulanceservice.mobile.data.booking.model.BookingStatus
import com.ambulanceservice.mobile.data.booking.repository.BookingListResult
import com.ambulanceservice.mobile.data.booking.repository.BookingLookupResult
import com.ambulanceservice.mobile.data.booking.repository.BookingRepository
import com.ambulanceservice.mobile.data.booking.repository.BookingSubmissionResult
import com.ambulanceservice.mobile.data.driver.repository.DriverRepository
import com.ambulanceservice.mobile.data.driver.repository.DriverResult
import com.ambulanceservice.mobile.di.AppContainer
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun DriverPortalScreen(
    authToken: String,
    onBackToBooking: () -> Unit,
    onLogout: () -> Unit
) {
    val driverRepository: DriverRepository = remember { AppContainer.driverRepository }
    val bookingRepository: BookingRepository = remember { AppContainer.bookingRepository }
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("UNAVAILABLE") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var availableBookings by remember { mutableStateOf<List<BookingResponse>>(emptyList()) }
    var activeTrip by remember { mutableStateOf<BookingResponse?>(null) }
    var tripMessage by remember { mutableStateOf<String?>(null) }
    var tripLoading by remember { mutableStateOf(false) }

    var showLocationEditor by remember { mutableStateOf(false) }

    val isAvailable = status.uppercase() == "AVAILABLE"
    val hasActiveTrip = activeTrip != null
    val prioritizedAvailableTrips = availableBookings
        .sortedByDescending { it.createdAt ?: LocalDateTime.MIN }
        .take(5)

    fun loadTripData() {
        scope.launch {
            tripLoading = true
            tripMessage = null

            when (val tripResult = bookingRepository.getDriverActiveTrip(authToken)) {
                is BookingLookupResult.Success -> activeTrip = tripResult.booking
                is BookingLookupResult.None -> activeTrip = null
                is BookingLookupResult.Error -> {
                    activeTrip = null
                    tripMessage = tripResult.message
                }
            }

            when (val bookingsResult = bookingRepository.getAvailableBookings(authToken)) {
                is BookingListResult.Success -> {
                    availableBookings = bookingsResult.bookings
                }
                is BookingListResult.Error -> {
                    availableBookings = emptyList()
                    if (tripMessage.isNullOrBlank()) {
                        tripMessage = bookingsResult.message
                    }
                }
            }

            tripLoading = false
        }
    }

    fun updateTripStatus(targetStatus: BookingStatus) {
        val tripId = activeTrip?.id ?: return

        scope.launch {
            tripLoading = true
            tripMessage = null

            when (val result = bookingRepository.updateBookingStatus(authToken, tripId, targetStatus)) {
                is BookingSubmissionResult.Success -> {
                    activeTrip = result.booking
                    tripMessage = "Trip status updated to ${result.booking.status}."
                    loadTripData()
                }
                is BookingSubmissionResult.Error -> tripMessage = result.message
            }

            tripLoading = false
        }
    }

    fun acceptBooking(bookingId: Long) {
        scope.launch {
            tripLoading = true
            tripMessage = null

            when (val result = bookingRepository.acceptBooking(authToken, bookingId)) {
                is BookingSubmissionResult.Success -> {
                    activeTrip = result.booking
                    tripMessage = "Trip accepted successfully."
                    loadTripData()
                }
                is BookingSubmissionResult.Error -> tripMessage = result.message
            }

            tripLoading = false
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            message = null
            when (val result = driverRepository.getAvailability(authToken)) {
                is DriverResult.Success -> {
                    status = result.driver.availabilityStatus ?: "UNAVAILABLE"
                    latitude = result.driver.latitude?.toString() ?: ""
                    longitude = result.driver.longitude?.toString() ?: ""
                }
                is DriverResult.Error -> message = result.message
            }

            loadTripData()
            isLoading = false
        }
    }

    fun updateAvailability(nextStatus: String) {
        scope.launch {
            isLoading = true
            message = null

            val latValue = latitude.toDoubleOrNull()
            val lngValue = longitude.toDoubleOrNull()

            val result = when (nextStatus) {
                "AVAILABLE" -> driverRepository.setAvailability(
                    token = authToken,
                    availabilityStatus = nextStatus,
                    latitude = latValue,
                    longitude = lngValue
                )
                else -> driverRepository.setAvailability(
                    token = authToken,
                    availabilityStatus = nextStatus,
                    latitude = null,
                    longitude = null
                )
            }

            when (result) {
                is DriverResult.Success -> {
                    status = result.driver.availabilityStatus ?: nextStatus
                    latitude = result.driver.latitude?.toString() ?: latitude
                    longitude = result.driver.longitude?.toString() ?: longitude
                    message = if (nextStatus == "AVAILABLE") {
                        "You are now available for dispatch."
                    } else {
                        "You are now unavailable and will not receive dispatch requests."
                    }
                }
                is DriverResult.Error -> message = result.message
            }

            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Driver portal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Set your current availability status and location so dispatch can find you when you are online.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Current availability status: ${if (isAvailable) "Available" else "Unavailable"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { if (!isAvailable) updateAvailability("AVAILABLE") },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && !isAvailable,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Available")
                    }

                    OutlinedButton(
                        onClick = { if (isAvailable) updateAvailability("UNAVAILABLE") },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && isAvailable,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Unavailable")
                    }
                }

                if (!showLocationEditor) {
                    Button(
                        onClick = { showLocationEditor = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Update location")
                    }
                } else {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = !isLoading
                    )

                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = !isLoading
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLocationEditor = false },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val lat = latitude.toDoubleOrNull()
                                val lng = longitude.toDoubleOrNull()
                                if (lat == null || lng == null) {
                                    message = "Please enter valid latitude and longitude values."
                                    return@Button
                                }

                                scope.launch {
                                    isLoading = true
                                    message = null
                                    when (val result = driverRepository.updateLocation(authToken, lat, lng)) {
                                        is DriverResult.Success -> {
                                            status = result.driver.availabilityStatus ?: status
                                            latitude = result.driver.latitude?.toString() ?: latitude
                                            longitude = result.driver.longitude?.toString() ?: longitude
                                            showLocationEditor = false
                                            message = "Location updated successfully."
                                        }
                                        is DriverResult.Error -> message = result.message
                                    }
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }

            }
        }

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
                    text = if (activeTrip != null) "Active trip" else "Trip management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (tripLoading) {
                    Text("Loading trip details...")
                }

                if (activeTrip != null) {
                    val trip = activeTrip!!
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Trip #${trip.id}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TripStatusBadge(status = trip.status)
                            }

                            Text(
                                text = "Patient: ${trip.contactName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = "${trip.pickupAddress} → ${trip.destinationAddress}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (trip.status == BookingStatus.ASSIGNED) {
                                    Button(
                                        onClick = { updateTripStatus(BookingStatus.IN_TRANSIT) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Start trip")
                                    }
                                }

                                if (trip.status == BookingStatus.IN_TRANSIT) {
                                    Button(
                                        onClick = { updateTripStatus(BookingStatus.ARRIVED) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Arrived")
                                    }
                                }

                                if (trip.status == BookingStatus.ARRIVED) {
                                    Button(
                                        onClick = { updateTripStatus(BookingStatus.COMPLETED) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Complete")
                                    }
                                }

                                if (trip.status in listOf(
                                        BookingStatus.ASSIGNED,
                                        BookingStatus.IN_TRANSIT,
                                        BookingStatus.ARRIVED
                                    )) {
                                    OutlinedButton(
                                        onClick = { updateTripStatus(BookingStatus.CANCELLED) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (availableBookings.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                        ) {
                            Text(
                                text = "No active trip\nNo available trips right now.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Available trips",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        prioritizedAvailableTrips.forEach { booking ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${booking.id}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        TripStatusBadge(status = BookingStatus.REQUESTED)
                                    }

                                    Text(
                                        text = booking.contactName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = "${booking.pickupAddress} → ${booking.destinationAddress}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Button(
                                        onClick = { if (!hasActiveTrip) acceptBooking(booking.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !tripLoading && !hasActiveTrip
                                    ) {
                                        Text(if (hasActiveTrip) "Trip already active" else "Accept trip")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (message != null) {
            Text(
                text = message ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = if (message!!.startsWith("You are now") || message!!.contains("successfully")) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }

        if (tripMessage != null) {
            Text(
                text = tripMessage ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = if (tripMessage!!.contains("successfully") || tripMessage!!.contains("updated")) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBackToBooking,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to booking")
            }
            Button(
                onClick = onLogout,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Log out")
            }
        }
    }
}

@Composable
private fun TripStatusBadge(status: BookingStatus) {
    val (label, containerColor, contentColor) = when (status) {
        BookingStatus.REQUESTED -> Triple("Available", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        BookingStatus.ASSIGNED -> Triple("Assigned", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        BookingStatus.IN_TRANSIT -> Triple("In transit", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        BookingStatus.ARRIVED -> Triple("Arrived", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        BookingStatus.COMPLETED -> Triple("Completed", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        BookingStatus.CANCELLED -> Triple("Cancelled", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        tonalElevation = 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier
                .background(containerColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}
