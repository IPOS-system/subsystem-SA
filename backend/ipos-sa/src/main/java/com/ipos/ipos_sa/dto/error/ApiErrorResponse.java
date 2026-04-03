package com.ipos.ipos_sa.dto.error;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Standard error response body returned by GlobalExceptionHandler.
 * Every non-2xx response from the API will have this shape, making it
 * straightforward for the C# frontend to parse and display error messages.
 *
 * Example:
 * {
 *   "timestamp": "2026-04-01T14:32:00",
 *   "status": 403,
 *   "error": "ORDER_REJECTED",
 *   "message": "Merchant account is suspended — no new orders accepted."
 * }
 */
@Data
@Builder
public class ApiErrorResponse {

    private LocalDateTime timestamp;

    /** HTTP status code. */
    private int status;

    /**
     * Machine-readable error code for the C# frontend to switch on.
     * Examples: "INVALID_CREDENTIALS", "ORDER_REJECTED",
     *           "INSUFFICIENT_STOCK", "ACCOUNT_INACTIVE", "NOT_FOUND"
     */
    private String error;

    /** Human-readable message describing what went wrong. */
    private String message;
}
