package com.ambulanceservice.mobile.data.driver.model

data class DriverAvailabilityRequest(
    val availabilityStatus: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class DriverLocationUpdateRequest(
    val latitude: Double,
    val longitude: Double
)

data class DriverAvailabilityResponse(
    val id: Long? = null,
    val fullName: String? = null,
    val email: String? = null,
    val availabilityStatus: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lastLocationUpdate: String? = null,
    val ambulanceRegistrationNumber: String? = null
)

enum class DriverAvailabilityStatus {
    AVAILABLE,
    UNAVAILABLE;

    companion object {
        fun fromBackend(value: String?): DriverAvailabilityStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: UNAVAILABLE
    }
}

