package com.example.parking.event;

/** Published after an active booking is cancelled and its slot is released. */
public record BookingCancelledEvent(Long bookingId) {
}
