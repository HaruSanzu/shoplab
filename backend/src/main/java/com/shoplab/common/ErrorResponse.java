package com.shoplab.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * The single JSON shape of EVERY error the API returns. One predictable format makes
 * API tests simple: always check status, message and (for validation) fieldErrors.
 *
 * <pre>
 * {
 *   "timestamp": "2026-11-11T10:00:00Z",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "path": "/api/auth/register",
 *   "fieldErrors": { "email": "Enter a valid email address" }
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
