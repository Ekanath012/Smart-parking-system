package com.example.parking.exception;

/**
 * Thrown when registration tries to reuse an existing email address.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
