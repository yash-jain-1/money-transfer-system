package com.moneytransfer.domain.exception;

/**
 * Exception thrown when a user attempts to access a resource they don't own.
 */
public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
