package com.ambulanceservice.mobile.data.booking.network

import com.ambulanceservice.mobile.data.booking.model.BookingResponse
import com.ambulanceservice.mobile.data.booking.model.CreateBookingRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BookingApiService {
    @POST("api/v1/bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): Response<BookingResponse>

    @GET("api/v1/bookings/{id}")
    suspend fun getBookingById(@Path("id") id: Long): Response<BookingResponse>
}
