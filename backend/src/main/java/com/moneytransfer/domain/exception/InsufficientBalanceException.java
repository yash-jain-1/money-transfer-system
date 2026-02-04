package com.moneytransfer.domain.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when account balance is insufficient for the transfer amount.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }

    public InsufficientBalanceException(BigDecimal balance, BigDecimal requiredAmount) {
        super("Insufficient balance. Available: " + balance + ", Required: " + requiredAmount);
    }
}
