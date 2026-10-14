package com.ambulanceservice.booking.entity;

public enum AmbulanceType {
  BASIC_LIFE_SUPPORT,      // Standard stretcher, oxygen, first aid
  ADVANCED_LIFE_SUPPORT,   // Paramedic onboard, ECG, IV lines
  ICU_VENTILATOR,          // Mobile intensive care with ventilator
  PATIENT_TRANSPORT        // Non-emergency transport
}
