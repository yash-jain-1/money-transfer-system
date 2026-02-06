package com.moneytransfer.domain.entity;

import com.moneytransfer.domain.status.AccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Account entity representing a user's bank account.
 * 
 * Uses optimistic locking (@Version) to prevent lost updates during concurrent transactions.
 * All balance mutations go through debit() and credit() methods to enforce business rules.
 * Each account is owned by a User (many-to-one relationship).
 */
@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String accountHolder;

    /**
     * The user who owns this account.
     * Many accounts can belong to one user.
     * Uses lazy loading to avoid loading user data when not needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true) // nullable for backward compatibility with existing data
    private User owner;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private String accountType;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Version field for optimistic locking.
     * Prevents lost updates when two concurrent transactions modify the same account.
     * JPA automatically increments this on each update.
     */
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Debits (withdraws) money from the account.
     * Validates that account is active and has sufficient balance.
     *
     * @param amount the amount to debit
     * @throws IllegalArgumentException if amount is invalid
     * @throws IllegalStateException if account is not active or has insufficient balance
     */
    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be greater than zero");
        }

        if (!isActive()) {
            throw new IllegalStateException("Account is not active. Current status: " + this.status);
        }

        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient balance. Available: " + this.balance + ", Required: " + amount);
        }

        this.balance = this.balance.subtract(amount);
    }

    /**
     * Credits (deposits) money to the account.
     * Validates that account is active and amount is valid.
     *
     * @param amount the amount to credit
     * @throws IllegalArgumentException if amount is invalid
     * @throws IllegalStateException if account is not active
     */
    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero");
        }

        if (!isActive()) {
            throw new IllegalStateException("Account is not active. Current status: " + this.status);
        }

        this.balance = this.balance.add(amount);
    }

    /**
     * Checks if the account is active.
     *
     * @return true if account status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return AccountStatus.ACTIVE.name().equals(this.status);
    }

    /**
     * Checks if the account has sufficient balance for a transaction.
     *
     * @param amount the amount to check
     * @return true if balance is greater than or equal to the amount
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.balance != null && this.balance.compareTo(amount) >= 0;
    }
}

