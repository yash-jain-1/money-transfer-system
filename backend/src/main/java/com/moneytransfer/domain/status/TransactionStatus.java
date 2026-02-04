package com.moneytransfer.domain.status;

/**
 * Enum representing the type and status of a transaction.
 */
public enum TransactionStatus {
    PENDING("Pending"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    REVERSED("Reversed"),
    CANCELLED("Cancelled");

    private final String statusLabel;

    TransactionStatus(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getStatusLabel() {
        return statusLabel;
    }
}
