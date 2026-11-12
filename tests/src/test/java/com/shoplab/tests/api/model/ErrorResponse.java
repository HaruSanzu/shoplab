package com.shoplab.tests.api.model;

import java.util.Map;

/**
 * Every error of the API has this shape, so negative tests can check it the same way:
 * <pre>
 * { "status": 400, "error": "Bad Request", "message": "Validation failed",
 *   "path": "/api/auth/register", "fieldErrors": { "email": "Enter a valid email address" } }
 * </pre>
 * fieldErrors is present only for validation errors.
 */
public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
