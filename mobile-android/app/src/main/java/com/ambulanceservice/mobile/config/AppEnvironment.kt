package com.ambulanceservice.mobile.config

import com.ambulanceservice.mobile.BuildConfig

object AppEnvironment {
    val name: String = BuildConfig.ENVIRONMENT_NAME
    val baseUrl: String = BuildConfig.API_BASE_URL
    val emergencyPhoneNumber: String = BuildConfig.EMERGENCY_PHONE_NUMBER
    val googleMapsConfigured: Boolean = BuildConfig.GOOGLE_MAPS_CONFIGURED

    val isLocal: Boolean
        get() = name.equals("Local", ignoreCase = true)
}
