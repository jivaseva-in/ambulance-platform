package com.ambulanceservice.mobile.data.booking.network

import com.ambulanceservice.mobile.BuildConfig
import com.ambulanceservice.mobile.config.AppEnvironment
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
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val bookingApiService: BookingApiService by lazy {
        retrofit.create(BookingApiService::class.java)
    }

    val bookingTrackingApiService: BookingTrackingApiService by lazy {
        retrofit.create(BookingTrackingApiService::class.java)
    }
}
