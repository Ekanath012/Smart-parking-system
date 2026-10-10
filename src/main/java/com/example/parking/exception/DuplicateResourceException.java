package com.example.parking.exception;

/**
 * Exception thrown when attempting to create a entity that already exists (e.g. duplicate email or slot number).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
