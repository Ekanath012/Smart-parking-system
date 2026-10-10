package com.example.parking.exception;

/**
 * Thrown when a slot can no longer be reserved because another transaction already claimed it.
 */
public class SlotAlreadyBookedException extends RuntimeException {

    public SlotAlreadyBookedException(String message) {
        super(message);
    }

    public SlotAlreadyBookedException(String message, Throwable cause) {
        super(message, cause);
    }
}
