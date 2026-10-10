package com.example.parking.event;

/** Published inside the booking transaction; handled only after that transaction commits. */
public record BookingCreatedEvent(Long bookingId) {
}
