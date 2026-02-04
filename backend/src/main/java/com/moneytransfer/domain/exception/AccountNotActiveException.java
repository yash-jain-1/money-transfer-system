package com.moneytransfer.domain.exception;

/**
 * Exception thrown when an account is not active (LOCKED/CLOSED).
 */
public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException(String message) {
        super(message);
    }

    public AccountNotActiveException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountNotActiveException(String accountNumber, String status) {
        super("Account " + accountNumber + " is " + status);
    }
}
