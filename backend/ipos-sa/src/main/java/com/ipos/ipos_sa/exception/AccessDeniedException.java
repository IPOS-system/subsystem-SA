package com.ipos.ipos_sa.exception;

/**
 * Thrown when the authenticated user's role is not permitted to perform
 * the requested action. Mapped to HTTP 403 by GlobalExceptionHandler.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}