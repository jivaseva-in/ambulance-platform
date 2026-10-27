package com.ambulanceservice.mobile.data.booking.network

import com.ambulanceservice.mobile.BuildConfig
import com.ambulanceservice.mobile.config.AppEnvironment
import com.ambulanceservice.mobile.data.auth.network.AuthApiService
import com.ambulanceservice.mobile.data.booking.model.LocalDateTimeAdapter
import com.ambulanceservice.mobile.data.driver.network.DriverApiService
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AppEnvironment.baseUrl)
            .client(httpClient)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder()
                        .registerTypeAdapter(java.time.LocalDateTime::class.java, LocalDateTimeAdapter())
                        .create()
                )
            )
            .build()
    }

    val bookingApiService: BookingApiService by lazy {
        retrofit.create(BookingApiService::class.java)
    }

    val bookingTrackingApiService: BookingTrackingApiService by lazy {
        retrofit.create(BookingTrackingApiService::class.java)
    }

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val driverApiService: DriverApiService by lazy {
        retrofit.create(DriverApiService::class.java)
    }
}
