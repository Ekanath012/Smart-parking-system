package com.example.parking.exception;

/**
 * Exception thrown when a booking rule constraint is violated (e.g. user already has active booking).
 */
public class BookingException extends RuntimeException {

    public BookingException(String message) {
        super(message);
    }
}
