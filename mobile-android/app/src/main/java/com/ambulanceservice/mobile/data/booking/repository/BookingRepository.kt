package com.ambulanceservice.mobile.data.booking.repository

import com.ambulanceservice.mobile.config.AppEnvironment
import com.ambulanceservice.mobile.data.booking.model.ApiErrorResponse
import com.ambulanceservice.mobile.data.booking.model.BookingResponse
import com.ambulanceservice.mobile.data.booking.model.CreateBookingRequest
import com.ambulanceservice.mobile.data.booking.network.BookingApiService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class BookingRepository(
    private val bookingApiService: BookingApiService,
    private val gson: Gson = Gson()
) {
    suspend fun createBooking(request: CreateBookingRequest): BookingSubmissionResult = withContext(Dispatchers.IO) {
        try {
            val response = bookingApiService.createBooking(request)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                BookingSubmissionResult.Success(body)
            } else {
                BookingSubmissionResult.Error(
                    parseErrorMessage(response.errorBody()?.string())
                        ?: "Booking request failed with code ${response.code()}."
                )
            }
        } catch (_: IOException) {
            val baseHint = if (AppEnvironment.isLocal) {
                " Ensure the backend is running and reachable at ${AppEnvironment.baseUrl}."
            } else {
                " Please check your internet connection and backend deployment URL."
            }
            BookingSubmissionResult.Error("Could not connect to the ambulance service backend.$baseHint")
        } catch (exception: Exception) {
            BookingSubmissionResult.Error(exception.message ?: "Unexpected error while creating booking.")
        }
    }

    suspend fun getBookingById(bookingId: Long): BookingLookupResult = withContext(Dispatchers.IO) {
        try {
            val response = bookingApiService.getBookingById(bookingId)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                BookingLookupResult.Success(body)
            } else {
                BookingLookupResult.Error(
                    parseErrorMessage(response.errorBody()?.string())
                        ?: "Could not find booking #$bookingId."
                )
            }
        } catch (_: IOException) {
            val baseHint = if (AppEnvironment.isLocal) {
                " Ensure the backend is running and reachable at ${AppEnvironment.baseUrl}."
            } else {
                " Please check your internet connection and backend deployment URL."
            }
            BookingLookupResult.Error("Could not connect to the ambulance service backend.$baseHint")
        } catch (exception: Exception) {
            BookingLookupResult.Error(exception.message ?: "Unexpected error while fetching booking details.")
        }
    }

    private fun parseErrorMessage(rawError: String?): String? {
        if (rawError.isNullOrBlank()) return null

        val parsedMessage = runCatching {
            gson.fromJson(rawError, ApiErrorResponse::class.java)?.message
        }.getOrNull()

        return parsedMessage?.takeIf { it.isNotBlank() } ?: rawError
    }
}

sealed interface BookingSubmissionResult {
    data class Success(val booking: BookingResponse) : BookingSubmissionResult
    data class Error(val message: String) : BookingSubmissionResult
}

sealed interface BookingLookupResult {
    data class Success(val booking: BookingResponse) : BookingLookupResult
    data class Error(val message: String) : BookingLookupResult
}
