package com.example.parking.event;

/** Published inside the checkout transaction; handled only after that transaction commits. */
public record ParkingCompletedEvent(Long bookingId) {
}
