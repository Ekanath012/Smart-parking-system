package com.example.parking.exception;

/**
 * Thrown when a booking transition is not allowed.
 */
public class BookingStateException extends RuntimeException {

    public BookingStateException(String message) {
        super(message);
    }
}
