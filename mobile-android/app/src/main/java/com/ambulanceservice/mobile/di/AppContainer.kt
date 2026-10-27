package com.ambulanceservice.mobile.di

import com.ambulanceservice.mobile.data.auth.network.AuthApiService
import com.ambulanceservice.mobile.data.auth.repository.AuthRepository
import com.ambulanceservice.mobile.data.booking.network.ApiClient
import com.ambulanceservice.mobile.data.booking.repository.BookingRepository
import com.ambulanceservice.mobile.data.booking.repository.BookingTrackingRepository
import com.ambulanceservice.mobile.data.driver.network.DriverApiService
import com.ambulanceservice.mobile.data.driver.repository.DriverRepository

object AppContainer {
    val bookingRepository: BookingRepository by lazy {
        BookingRepository(ApiClient.bookingApiService)
    }

    val bookingTrackingRepository: BookingTrackingRepository by lazy {
        BookingTrackingRepository(ApiClient.bookingTrackingApiService)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(ApiClient.authApiService)
    }

    val driverRepository: DriverRepository by lazy {
        DriverRepository(ApiClient.driverApiService)
    }
}
