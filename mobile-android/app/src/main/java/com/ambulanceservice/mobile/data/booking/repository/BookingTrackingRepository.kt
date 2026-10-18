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

