package com.ambulanceservice.mobile.data.booking.model

enum class AmbulanceType(val displayName: String) {
    BASIC_LIFE_SUPPORT("Basic Life Support"),
    ADVANCED_LIFE_SUPPORT("Advanced Life Support"),
    ICU_VENTILATOR("ICU / Ventilator"),
    PATIENT_TRANSPORT("Patient Transport")
}

