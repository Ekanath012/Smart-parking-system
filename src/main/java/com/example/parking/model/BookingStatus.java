package com.example.parking.model;

/**
 * Status lifecycle of a slot booking.
 * Includes legacy and active status values for 100% database compatibility.
 */
public enum BookingStatus {
    BOOKED,
    CHECKED_IN,
    ACTIVE,
    RESERVED,
    COMPLETED,
    CANCELLED
}
