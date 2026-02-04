package com.moneytransfer.domain.status;

/**
 * Enum representing the status of an account.
 */
public enum AccountStatus {
    ACTIVE("Active"),
    LOCKED("Locked"),
    CLOSED("Closed"),
    SUSPENDED("Suspended");

    private final String statusLabel;

    AccountStatus(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getStatusLabel() {
        return statusLabel;
    }
}
