package com.ambulanceservice.mobile.data.booking.network

import com.ambulanceservice.mobile.data.booking.model.BookingResponse
import com.ambulanceservice.mobile.data.booking.model.CreateBookingRequest
import com.ambulanceservice.mobile.data.booking.model.UpdateBookingStatusRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface BookingApiService {
    @POST("api/v1/bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): Response<BookingResponse>

    @GET("api/v1/bookings/{id}")
    suspend fun getBookingById(@Path("id") id: Long): Response<BookingResponse>

    @GET("api/v1/bookings/available")
    suspend fun getAvailableBookings(
        @Header("Authorization") token: String
    ): Response<List<BookingResponse>>

    @PUT("api/v1/bookings/{id}/accept")
    suspend fun acceptBooking(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<BookingResponse>

    @PUT("api/v1/bookings/{id}/confirm")
    suspend fun confirmBooking(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<BookingResponse>

    @PUT("api/v1/bookings/{id}/negotiation-failed")
    suspend fun failNegotiation(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Query("reason") reason: String? = null
    ): Response<BookingResponse>

    @PUT("api/v1/bookings/{id}/cancel-request")
    suspend fun cancelBookingRequest(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Query("reason") reason: String? = null
    ): Response<BookingResponse>

    @PUT("api/v1/bookings/{id}/status")
    suspend fun updateBookingStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: UpdateBookingStatusRequest
    ): Response<BookingResponse>

    @GET("api/v1/bookings/driver/active")
    suspend fun getDriverActiveTrip(
        @Header("Authorization") token: String
    ): Response<BookingResponse>

    @GET("api/v1/bookings/my")
    suspend fun getMyBookings(
        @Header("Authorization") token: String
    ): Response<List<BookingResponse>>
}
