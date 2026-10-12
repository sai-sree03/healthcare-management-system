package com.healthcare.hms.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** The one JSON shape every error response uses. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {
}
