package com.ipos.ipos_sa.exception;

/**
 * Thrown when a requested entity does not exist in the database.
 * Mapped to HTTP 404 by GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String entityName, Object id) {
        super(entityName + " not found with ID: " + id);
    }
}