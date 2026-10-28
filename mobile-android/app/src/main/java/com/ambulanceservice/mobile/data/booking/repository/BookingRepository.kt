package com.ambulanceservice.mobile.data.booking.repository

import com.ambulanceservice.mobile.config.AppEnvironment
import com.ambulanceservice.mobile.data.booking.model.ApiErrorResponse
import com.ambulanceservice.mobile.data.booking.model.BookingResponse
import com.ambulanceservice.mobile.data.booking.model.BookingStatus
import com.ambulanceservice.mobile.data.booking.model.CreateBookingRequest
import com.ambulanceservice.mobile.data.booking.model.UpdateBookingStatusRequest
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

    suspend fun getAvailableBookings(token: String): BookingListResult = withContext(Dispatchers.IO) {
        try {
            val bearerToken = ensureBearerToken(token)
            val response = bookingApiService.getAvailableBookings(bearerToken)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                BookingListResult.Success(body)
            } else {
                BookingListResult.Error(
                    parseErrorMessage(response.errorBody()?.string())
                        ?: "Could not load available bookings."
                )
            }
        } catch (_: IOException) {
            BookingListResult.Error("Could not connect to the ambulance platform backend.")
        } catch (exception: Exception) {
            BookingListResult.Error(exception.message ?: "Unexpected booking list error.")
        }
    }

    suspend fun acceptBooking(token: String, bookingId: Long): BookingSubmissionResult = withContext(Dispatchers.IO) {
        try {
            val bearerToken = ensureBearerToken(token)
            val response = bookingApiService.acceptBooking(bearerToken, bookingId)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                BookingSubmissionResult.Success(body)
            } else {
                BookingSubmissionResult.Error(
                    parseErrorMessage(response.errorBody()?.string())
                        ?: "Could not accept booking #$bookingId."
                )
            }
        } catch (_: IOException) {
            BookingSubmissionResult.Error("Could not connect to the ambulance platform backend.")
        } catch (exception: Exception) {
            BookingSubmissionResult.Error(exception.message ?: "Unexpected accept booking error.")
        }
    }

    suspend fun updateBookingStatus(
        token: String,
        bookingId: Long,
        status: BookingStatus,
        cancellationReason: String? = null
    ): BookingSubmissionResult = withContext(Dispatchers.IO) {
        try {
            val bearerToken = ensureBearerToken(token)
            val response = bookingApiService.updateBookingStatus(
                token = bearerToken,
                id = bookingId,
                request = UpdateBookingStatusRequest(status = status, cancellationReason = cancellationReason)
            )
            val body = response.body()

            if (response.isSuccessful && body != null) {
                BookingSubmissionResult.Success(body)
            } else {
                BookingSubmissionResult.Error(
                    parseErrorMessage(response.errorBody()?.string())
                        ?: "Could not update booking status."
                )
            }
        } catch (_: IOException) {
            BookingSubmissionResult.Error("Could not connect to the ambulance platform backend.")
        } catch (exception: Exception) {
            BookingSubmissionResult.Error(exception.message ?: "Unexpected booking update error.")
        }
    }

    suspend fun confirmBooking(token: String, bookingId: Long): BookingSubmissionResult = withContext(Dispatchers.IO) {
        try {
            val bearerToken = ensureBearerToken(token)
            val response = bookingApiService.confirmBooking(bearerToken, bookingId)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                BookingSubmissionResult.Success(body)
            } else {
                BookingSubmissionResult.Error(
                    parseErrorMessage(response.errorBody()?.string())
                        ?: "Could not confirm booking #$bookingId."
                )
            }
        } catch (_: IOException) {
            BookingSubmissionResult.Error("Could not connect to the ambulance platform backend.")
        } catch (exception: Exception) {
            BookingSubmissionResult.Error(exception.message ?: "Unexpected confirm booking error.")
        }
    }

    suspend fun failNegotiation(token: String, bookingId: Long, reason: String? = null): BookingSubmissionResult = withContext(Dispatchers.IO) {
        try {
            val bearerToken = ensureBearerToken(token)
            val response = bookingApiService.failNegotiation(bearerToken, bookingId, reason)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                BookingSubmissionResult.Success(body)
            } else {
                BookingSubmissionResult.Error(
                    parseErrorMessage(response.errorBody()?.string())
                        ?: "Could not fail negotiation for booking #$bookingId."
                )
            }
        } catch (_: IOException) {
            BookingSubmissionResult.Error("Could not connect to the ambulance platform backend.")
        } catch (exception: Exception) {
            BookingSubmissionResult.Error(exception.message ?: "Unexpected negotiation failure error.")
        }
    }

    suspend fun cancelBookingRequest(token: String, bookingId: Long, reason: String? = null): BookingSubmissionResult = withContext(Dispatchers.IO) {
        try {
            val bearerToken = ensureBearerToken(token)
            val response = bookingApiService.cancelBookingRequest(bearerToken, bookingId, reason)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                BookingSubmissionResult.Success(body)
            } else {
                BookingSubmissionResult.Error(
                    parseErrorMessage(response.errorBody()?.string())
                        ?: "Could not cancel booking request #$bookingId."
                )
            }
        } catch (_: IOException) {
            BookingSubmissionResult.Error("Could not connect to the ambulance platform backend.")
        } catch (exception: Exception) {
            BookingSubmissionResult.Error(exception.message ?: "Unexpected booking cancel request error.")
        }
    }

    suspend fun getDriverActiveTrip(token: String): BookingLookupResult = withContext(Dispatchers.IO) {
        try {
            val bearerToken = ensureBearerToken(token)
            val response = bookingApiService.getDriverActiveTrip(bearerToken)

            if (response.code() == 204) {
                return@withContext BookingLookupResult.None
            }

            val body = response.body()
            if (response.isSuccessful && body != null) {
                BookingLookupResult.Success(body)
            } else {
                BookingLookupResult.Error(
                    parseErrorMessage(response.errorBody()?.string())
                        ?: "No active trip found."
                )
            }
        } catch (_: IOException) {
            BookingLookupResult.Error("Could not connect to the ambulance platform backend.")
        } catch (exception: Exception) {
            BookingLookupResult.Error(exception.message ?: "Unexpected trip lookup error.")
        }
    }

    suspend fun getMyBookings(token: String): BookingListResult = withContext(Dispatchers.IO) {
        try {
            val bearerToken = ensureBearerToken(token)
            val response = bookingApiService.getMyBookings(bearerToken)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                BookingListResult.Success(body)
            } else {
                BookingListResult.Error(
                    parseErrorMessage(response.errorBody()?.string())
                        ?: "Could not load your bookings."
                )
            }
        } catch (_: IOException) {
            BookingListResult.Error("Could not connect to the ambulance platform backend.")
        } catch (exception: Exception) {
            BookingListResult.Error(exception.message ?: "Unexpected booking history error.")
        }
    }

    private fun ensureBearerToken(token: String): String =
        if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"

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
    data object None : BookingLookupResult
}

sealed interface BookingListResult {
    data class Success(val bookings: List<BookingResponse>) : BookingListResult
    data class Error(val message: String) : BookingListResult
}
