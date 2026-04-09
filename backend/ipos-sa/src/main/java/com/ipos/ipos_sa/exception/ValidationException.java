package com.ipos.ipos_sa.exception;

/**
 * Thrown when a business rule is violated (e.g. ordering while suspended). Mapped to HTTP 400 by
 * GlobalExceptionHandler.
 */
public class ValidationException extends RuntimeException {

  public ValidationException(String message) {
    super(message);
  }
}
