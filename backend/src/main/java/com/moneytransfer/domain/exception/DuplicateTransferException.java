package com.moneytransfer.domain.exception;

/**
 * Exception thrown when an idempotency key is reused (duplicate transfer).
 */
public class DuplicateTransferException extends RuntimeException {

    public DuplicateTransferException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateTransferException(String idempotencyKey) {
        super("Idempotency key '" + idempotencyKey + "' has already been used. Duplicate transfer detected.");
    }
}
