package com.ipos.ipos_sa.exception;

/**
 * Thrown when attempting to create an account with a username that already exists. Mapped to HTTP
 * 409 by GlobalExceptionHandler.
 */
public class DuplicateUsernameException extends RuntimeException {

  public DuplicateUsernameException(String username) {
    super("Username already exists: " + username);
  }
}
