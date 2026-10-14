package com.ambulanceservice.booking.entity;

public enum BookingChannel {
  ONLINE_WEB,      // Browser online booking
  ONLINE_MOBILE,   // Android / iOS app
  PHONE_DISPATCH,  // Call center dispatcher entered on caller's behalf
  WHATSAPP         // WhatsApp webhook or bot assisted
}
