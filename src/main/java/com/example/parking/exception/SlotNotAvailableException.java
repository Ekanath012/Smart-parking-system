package com.example.parking.exception;

/**
 * Exception thrown when a user attempts to book a slot that is already occupied or under maintenance.
 */
public class SlotNotAvailableException extends RuntimeException {

    public SlotNotAvailableException(String message) {
        super(message);
    }
}
