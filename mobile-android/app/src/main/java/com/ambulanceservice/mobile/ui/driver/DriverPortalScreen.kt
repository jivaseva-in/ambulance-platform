package com.ambulanceservice.mobile.ui.driver

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
private fun ClickablePhoneText(phone: String?) {
    val context = LocalContext.current
    val sanitizedPhone = phone?.replace(Regex("[^+\\d]"), "")?.trim()

    Text(
        text = if (sanitizedPhone.isNullOrBlank()) "Call: unavailable" else "Call: $sanitizedPhone",
        style = MaterialTheme.typography.bodyMedium,
        color = if (sanitizedPhone.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable(enabled = !sanitizedPhone.isNullOrBlank()) {
            val dialIntent = Intent(Intent.ACTION_DIAL, "tel:$sanitizedPhone".toUri())
            context.startActivity(dialIntent)
        }
    )
}

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

    val visibleAvailableTrips = availableBookings
        .filterNot { it.id == activeTrip?.id }
        .filter { booking ->
            booking.status in listOf(
                BookingStatus.REQUESTED,
                BookingStatus.REASSIGNMENT_PENDING,
                BookingStatus.NEGOTIATION_FAILED,
                BookingStatus.CANCEL_REQUESTED
            )
        }
        .sortedByDescending { it.createdAt ?: LocalDateTime.MIN }
        .take(5)

    val activeTripStatuses = setOf(
        BookingStatus.DRIVER_HELD,
        BookingStatus.CONFIRMED,
        BookingStatus.ASSIGNED,
        BookingStatus.IN_TRANSIT,
        BookingStatus.ARRIVED
    )

    fun shouldKeepActiveTrip(status: BookingStatus?): Boolean =
        status in activeTripStatuses

    val isAvailable = status.equals("AVAILABLE", ignoreCase = true)
    val hasActiveTrip = activeTrip != null
    val canAcceptTrips = isAvailable && !hasActiveTrip && !tripLoading
    val prioritizedAvailableTrips = visibleAvailableTrips

    fun loadTripData() {
        scope.launch {
            tripLoading = true
            tripMessage = null

            when (val tripResult = bookingRepository.getDriverActiveTrip(authToken)) {
                is BookingLookupResult.Success -> activeTrip = tripResult.booking
                is BookingLookupResult.None -> {
                    val currentTripStatus = activeTrip?.status
                    val shouldKeepCurrentTrip = currentTripStatus?.let { shouldKeepActiveTrip(it) } ?: false
                    if (!shouldKeepCurrentTrip) {
                        activeTrip = null
                    }
                }
                is BookingLookupResult.Error -> {
                    activeTrip = null
                    tripMessage = tripResult.message
                }
            }

            when (val bookingsResult = bookingRepository.getAvailableBookings(authToken)) {
                is BookingListResult.Success -> availableBookings = bookingsResult.bookings
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
                    availableBookings = availableBookings.filterNot { it.id == bookingId }
                    tripMessage = "Trip accepted. Review the active trip and either confirm it or mark negotiation failed."
                    loadTripData()
                }
                is BookingSubmissionResult.Error -> {
                    tripMessage = result.message
                    if (result.message.contains("Booking is not available for a new driver hold", ignoreCase = true)) {
                        activeTrip = null
                        loadTripData()
                    }
                }
            }

            tripLoading = false
        }
    }

    fun confirmHeldBooking() {
        val tripId = activeTrip?.id ?: return

        scope.launch {
            tripLoading = true
            tripMessage = null

            when (val result = bookingRepository.confirmBooking(authToken, tripId)) {
                is BookingSubmissionResult.Success -> {
                    activeTrip = result.booking
                    availableBookings = availableBookings.filterNot { it.id == tripId }
                    tripMessage = "Booking confirmed. You can now proceed to the trip."
                    loadTripData()
                }
                is BookingSubmissionResult.Error -> tripMessage = result.message
            }

            tripLoading = false
        }
    }

    fun failNegotiation(reason: String? = null) {
        val tripId = activeTrip?.id ?: return

        scope.launch {
            tripLoading = true
            tripMessage = null

            when (val result = bookingRepository.failNegotiation(authToken, tripId, reason)) {
                is BookingSubmissionResult.Success -> {
                    activeTrip = null
                    val refreshedList = (listOfNotNull(result.booking) + availableBookings.filterNot { it.id == tripId })
                        .distinctBy { it.id }
                    availableBookings = refreshedList
                    tripMessage = "Negotiation failed. The request has been released for another driver."
                    loadTripData()
                }
                is BookingSubmissionResult.Error -> tripMessage = result.message
            }

            tripLoading = false
        }
    }

    fun cancelHeldBooking(reason: String? = null) {
        val tripId = activeTrip?.id ?: return

        scope.launch {
            tripLoading = true
            tripMessage = null

            when (val result = bookingRepository.cancelBookingRequest(authToken, tripId, reason)) {
                is BookingSubmissionResult.Success -> {
                    activeTrip = null
                    val refreshedList = (listOfNotNull(result.booking) + availableBookings.filterNot { it.id == tripId })
                        .distinctBy { it.id }
                    availableBookings = refreshedList
                    tripMessage = "Booking cancelled and released for reassignment."
                    loadTripData()
                }
                is BookingSubmissionResult.Error -> tripMessage = result.message
            }

            tripLoading = false
        }
    }

    fun recoverActiveTripFromAssignedBookings() {
        scope.launch {
            when (val myBookingsResult = bookingRepository.getMyBookings(authToken)) {
                is BookingListResult.Success -> {
                    val recovered = myBookingsResult.bookings
                        .filter { it.status in activeTripStatuses }
                        .sortedByDescending { it.createdAt ?: LocalDateTime.MIN }
                        .firstOrNull()

                    if (recovered != null) {
                        activeTrip = recovered
                    } else if (activeTrip?.status in activeTripStatuses) {
                        // Keep the current trip until the backend explicitly shows it has ended.
                    } else {
                        activeTrip = null
                    }
                }
                is BookingListResult.Error -> {
                    if (activeTrip?.status !in activeTripStatuses) {
                        activeTrip = null
                    }
                }
            }
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
            text = "Your job status, current trip, and available rides in one place.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val topAlertMessage = when {
            !tripMessage.isNullOrBlank() -> tripMessage
            !message.isNullOrBlank() -> message
            else -> null
        }
        if (!topAlertMessage.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = topAlertMessage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAvailable) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Driver status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    DriverStatusBadge(isAvailable)
                }

                Text(
                    text = if (isAvailable) {
                        "You are online and can receive jobs."
                    } else {
                        "You are offline and will not receive dispatch requests."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { if (!isAvailable) updateAvailability("AVAILABLE") },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && !isAvailable,
                        shape = RoundedCornerShape(12.dp)
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
                containerColor = if (hasActiveTrip) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (hasActiveTrip) "Active trip" else "Current trip",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (tripLoading) {
                    Text(
                        text = "Loading trip details...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (activeTrip != null) {
                    val trip = activeTrip!!

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
                        text = "Requester: ${trip.contactName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    ClickablePhoneText(trip.contactPhone)

                    Text(
                        text = "Pickup: ${trip.pickupAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Destination: ${trip.destinationAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val tripActionButtons: List<Pair<String, () -> Unit>> = buildList {
                        when (trip.status) {
                            BookingStatus.DRIVER_HELD -> {
                                add("Confirm price" to { confirmHeldBooking() })
                                add("Negotiation failed" to { failNegotiation() })
                                add("Cancel request" to { cancelHeldBooking() })
                            }
                            BookingStatus.CONFIRMED -> {
                                add("Start trip" to { updateTripStatus(BookingStatus.IN_TRANSIT) })
                                add("Cancel request" to { cancelHeldBooking() })
                            }
                            BookingStatus.ASSIGNED -> {
                                add("Start trip" to { updateTripStatus(BookingStatus.IN_TRANSIT) })
                                add("Cancel trip" to { updateTripStatus(BookingStatus.CANCELLED) })
                            }
                            BookingStatus.IN_TRANSIT -> {
                                add("Arrived" to { updateTripStatus(BookingStatus.ARRIVED) })
                                add("Cancel trip" to { updateTripStatus(BookingStatus.CANCELLED) })
                            }
                            BookingStatus.ARRIVED -> {
                                add("Complete" to { updateTripStatus(BookingStatus.COMPLETED) })
                                add("Cancel trip" to { updateTripStatus(BookingStatus.CANCELLED) })
                            }
                            else -> {
                                if (trip.status == BookingStatus.ASSIGNED) {
                                    add("Start trip" to { updateTripStatus(BookingStatus.IN_TRANSIT) })
                                }
                                if (trip.status == BookingStatus.IN_TRANSIT) {
                                    add("Arrived" to { updateTripStatus(BookingStatus.ARRIVED) })
                                }
                                if (trip.status == BookingStatus.ARRIVED) {
                                    add("Complete" to { updateTripStatus(BookingStatus.COMPLETED) })
                                }
                                if (trip.status in listOf(
                                        BookingStatus.ASSIGNED,
                                        BookingStatus.IN_TRANSIT,
                                        BookingStatus.ARRIVED,
                                        BookingStatus.CONFIRMED
                                    )) {
                                    add("Cancel trip" to { updateTripStatus(BookingStatus.CANCELLED) })
                                }
                            }
                        }
                    }

                    if (tripActionButtons.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tripActionButtons.chunked(2).forEach { rowButtons ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowButtons.forEach { (label, onClick) ->
                                        Button(
                                            onClick = onClick,
                                            modifier = Modifier.weight(1f),
                                            enabled = !tripLoading,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(label)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No active trip right now. When a booking is assigned to you, it will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available rides",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (prioritizedAvailableTrips.isEmpty()) "0" else prioritizedAvailableTrips.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (prioritizedAvailableTrips.isEmpty()) {
                    Text(
                        text = "No rides ready for pickup right now. Check back when the dispatch queue refreshes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
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
                                        text = "Ride #${booking.id}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    TripStatusBadge(status = booking.status)
                                }

                                Text(
                                    text = "Requester: ${booking.contactName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )

                                ClickablePhoneText(booking.contactPhone)

                                Text(
                                    text = "Pickup: ${booking.pickupAddress}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "Destination: ${booking.destinationAddress}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = { if (canAcceptTrips) acceptBooking(booking.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !tripLoading && canAcceptTrips,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        when {
                                            !isAvailable -> "Go online to accept"
                                            hasActiveTrip -> "Trip already active"
                                            else -> "Accept ride"
                                        }
                                    )
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
                color = if (message!!.startsWith("You are now") || message!!.contains("successfully") || message!!.contains("updated")) {
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
private fun DriverStatusBadge(isAvailable: Boolean) {
    val (label, containerColor, contentColor) = if (isAvailable) {
        Triple("Available", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
    } else {
        Triple("Unavailable", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
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
                .padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TripStatusBadge(status: BookingStatus) {
    val (label, containerColor, contentColor) = when (status) {
        BookingStatus.REQUESTED -> Triple("Open", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        BookingStatus.DRIVER_HELD -> Triple("Held", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        BookingStatus.NEGOTIATION_FAILED -> Triple("Retry", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        BookingStatus.REASSIGNMENT_PENDING -> Triple("Requeue", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        BookingStatus.CONFIRMED -> Triple("Confirmed", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        BookingStatus.ASSIGNED -> Triple("Assigned", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        BookingStatus.IN_TRANSIT -> Triple("In transit", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        BookingStatus.ARRIVED -> Triple("Arrived", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        BookingStatus.COMPLETED -> Triple("Completed", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        BookingStatus.CANCEL_REQUESTED -> Triple("Canceling", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        BookingStatus.CANCELLED -> Triple("Cancelled", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        BookingStatus.EXPIRED -> Triple("Expired", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
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
