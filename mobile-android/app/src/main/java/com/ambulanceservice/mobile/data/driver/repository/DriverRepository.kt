package com.ambulanceservice.mobile.data.driver.repository

import com.ambulanceservice.mobile.data.driver.model.DriverAvailabilityRequest
import com.ambulanceservice.mobile.data.driver.model.DriverAvailabilityResponse
import com.ambulanceservice.mobile.data.driver.model.DriverLocationUpdateRequest
import com.ambulanceservice.mobile.data.driver.network.DriverApiService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class DriverRepository(
    private val driverApiService: DriverApiService,
    private val gson: Gson = Gson()
) {
    suspend fun getAvailability(token: String): DriverResult = withContext(Dispatchers.IO) {
        try {
            val bearerToken = ensureBearerToken(token)
            val response = driverApiService.getAvailability(bearerToken)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                DriverResult.Success(body)
            } else {
                DriverResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "Could not load driver status.")
            }
        } catch (_: IOException) {
            DriverResult.Error("Could not connect to the ambulance platform backend.")
        } catch (exception: Exception) {
            DriverResult.Error(exception.message ?: "Unexpected driver status error.")
        }
    }

    suspend fun setAvailability(token: String, availabilityStatus: String, latitude: Double?, longitude: Double?): DriverResult = withContext(Dispatchers.IO) {
        try {
            val bearerToken = ensureBearerToken(token)
            val request = DriverAvailabilityRequest(availabilityStatus = availabilityStatus, latitude = latitude, longitude = longitude)
            val response = driverApiService.setAvailability(bearerToken, request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                DriverResult.Success(body)
            } else {
                DriverResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "Could not update driver availability.")
            }
        } catch (_: IOException) {
            DriverResult.Error("Could not connect to the ambulance platform backend.")
        } catch (exception: Exception) {
            DriverResult.Error(exception.message ?: "Unexpected availability update error.")
        }
    }

    suspend fun updateLocation(token: String, latitude: Double, longitude: Double): DriverResult = withContext(Dispatchers.IO) {
        try {
            val bearerToken = ensureBearerToken(token)
            val request = DriverLocationUpdateRequest(latitude = latitude, longitude = longitude)
            val response = driverApiService.updateLocation(bearerToken, request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                DriverResult.Success(body)
            } else {
                DriverResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "Could not update driver location.")
            }
        } catch (_: IOException) {
            DriverResult.Error("Could not connect to the ambulance platform backend.")
        } catch (exception: Exception) {
            DriverResult.Error(exception.message ?: "Unexpected location update error.")
        }
    }

    private fun ensureBearerToken(token: String): String =
        if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"

    private fun parseErrorMessage(rawError: String?): String? {
        if (rawError.isNullOrBlank()) return null
        return runCatching {
            val json = gson.fromJson(rawError, Map::class.java)
            val message = json["message"] as? String
            message ?: json["error"] as? String ?: rawError
        }.getOrElse { rawError }
    }
}

sealed interface DriverResult {
    data class Success(val driver: DriverAvailabilityResponse) : DriverResult
    data class Error(val message: String) : DriverResult
}

