package com.ipos.ipos_sa.exception;

import com.ipos.ipos_sa.dto.error.ApiErrorResponse;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised exception handler for the entire API. Catches all custom exceptions and Spring
 * validation errors, returning a consistent ApiErrorResponse JSON body so the C# frontend can parse
 * errors uniformly.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    log.warn("Resource not found: {}", ex.getMessage());
    return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException ex) {
    log.warn("Validation error: {}", ex.getMessage());
    return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage());
  }

  @ExceptionHandler(DuplicateUsernameException.class)
  public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateUsernameException ex) {
    log.warn("Duplicate username: {}", ex.getMessage());
    return buildResponse(HttpStatus.CONFLICT, "DUPLICATE_USERNAME", ex.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    log.warn("Access denied: {}", ex.getMessage());
    return buildResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage());
  }

  /**
   * Handles @Valid failures on request DTOs.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgNotValid(
      MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
    log.warn("Request validation failed: {}", message);
    return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
  }

  /**
   * Catch-all for any unexpected exception so the frontend never sees a raw Spring error page or
   * stack trace.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex) {
    log.error("Unhandled exception", ex);
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(
      HttpStatus status, String error, String message) {
    ApiErrorResponse body =
        ApiErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(error)
            .message(message)
            .build();
    return ResponseEntity.status(status).body(body);
  }
}
