package com.ambulanceservice.mobile.di

import com.ambulanceservice.mobile.data.booking.network.ApiClient
import com.ambulanceservice.mobile.data.booking.repository.BookingRepository
import com.ambulanceservice.mobile.data.booking.repository.BookingTrackingRepository

object AppContainer {
    val bookingRepository: BookingRepository by lazy {
        BookingRepository(ApiClient.bookingApiService)
    }

    val bookingTrackingRepository: BookingTrackingRepository by lazy {
        BookingTrackingRepository(ApiClient.bookingTrackingApiService)
    }
}
