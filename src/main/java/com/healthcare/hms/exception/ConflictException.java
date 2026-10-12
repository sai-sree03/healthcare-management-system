package com.healthcare.hms.exception;

/** Maps to HTTP 409 (duplicate email, double-booked slot, ...). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
