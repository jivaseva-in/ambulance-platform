package com.ambulanceservice.mobile.data.booking.repository

import com.ambulanceservice.mobile.config.AppEnvironment
import com.ambulanceservice.mobile.data.booking.model.LocationResponse
import com.ambulanceservice.mobile.data.booking.network.BookingTrackingApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class BookingTrackingRepository(
    private val bookingTrackingApiService: BookingTrackingApiService
) {
    suspend fun getBookingByPhone(phoneNumber: String): BookingLookupResult = withContext(Dispatchers.IO) {
        val normalizedPhone = phoneNumber.trim()

        try {
            val response = bookingTrackingApiService.getBookingByPhone(normalizedPhone)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                BookingLookupResult.Success(body)
            } else {
                BookingLookupResult.Error(
                    "Could not find any recent booking for $normalizedPhone.\n${response.message()}"
                )
            }
        } catch (_: IOException) {
            val hint = if (AppEnvironment.isLocal) {
                " Ensure the backend is running and reachable at ${AppEnvironment.baseUrl}."
            } else {
                " Please check your internet connection and backend deployment URL."
            }
            BookingLookupResult.Error("Could not connect to the ambulance service backend.$hint")
        } catch (exception: Exception) {
            BookingLookupResult.Error(exception.message ?: "Unexpected error while looking up booking by phone.")
        }
    }

    suspend fun getLiveLocation(bookingId: Long): TrackingResult = withContext(Dispatchers.IO) {
        try {
            val response = bookingTrackingApiService.getLiveLocation(bookingId)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                TrackingResult.Success(body)
            } else {
                TrackingResult.Error(
                    "Could not load live booking location for ID $bookingId.\n${response.message()}"
                )
            }
        } catch (_: IOException) {
            val hint = if (AppEnvironment.isLocal) {
                " Ensure the backend is running and reachable at ${AppEnvironment.baseUrl}."
            } else {
                " Please check your internet connection and backend deployment URL."
            }
            TrackingResult.Error("Could not connect to the ambulance service backend.$hint")
        } catch (exception: Exception) {
            TrackingResult.Error(exception.message ?: "Unexpected error while fetching tracking details.")
        }
    }
}

sealed interface TrackingResult {
    data class Success(val location: LocationResponse) : TrackingResult
    data class Error(val message: String) : TrackingResult
}
