package com.ambulanceservice.mobile.data.booking.network

import com.ambulanceservice.mobile.data.booking.model.LocationResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface BookingTrackingApiService {
    @GET("api/v1/bookings/{bookingId}/location")
    suspend fun getLiveLocation(@Path("bookingId") bookingId: Long): Response<LocationResponse>
}

