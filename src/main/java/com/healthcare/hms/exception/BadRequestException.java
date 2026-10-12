package com.healthcare.hms.exception;

/** Maps to HTTP 400 (business rule broken by the request). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
