package com.example.parking.exception;

/**
 * Exception thrown when a user attempts an action outside their role or ownership.
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
