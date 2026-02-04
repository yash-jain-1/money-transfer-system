package com.moneytransfer.domain.exception;

/**
 * Exception thrown when an account with the specified ID doesn't exist.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }

    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountNotFoundException(Long accountId) {
        super("Account ID " + accountId + " doesn't exist");
    }
}
