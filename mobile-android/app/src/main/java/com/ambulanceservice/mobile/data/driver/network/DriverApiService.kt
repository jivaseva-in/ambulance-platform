package com.ambulanceservice.mobile.data.driver.network

import com.ambulanceservice.mobile.data.driver.model.DriverAvailabilityRequest
import com.ambulanceservice.mobile.data.driver.model.DriverAvailabilityResponse
import com.ambulanceservice.mobile.data.driver.model.DriverLocationUpdateRequest
import retrofit2.Response
import retrofit2.http.*

interface DriverApiService {
    @GET("api/v1/drivers/me/availability")
    suspend fun getAvailability(
        @Header("Authorization") token: String
    ): Response<DriverAvailabilityResponse>

    @PUT("api/v1/drivers/me/availability")
    suspend fun setAvailability(
        @Header("Authorization") token: String,
        @Body request: DriverAvailabilityRequest
    ): Response<DriverAvailabilityResponse>

    @PUT("api/v1/drivers/me/location")
    suspend fun updateLocation(
        @Header("Authorization") token: String,
        @Body request: DriverLocationUpdateRequest
    ): Response<DriverAvailabilityResponse>
}

