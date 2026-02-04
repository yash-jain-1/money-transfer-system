package com.moneytransfer.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TransactionLog entity for audit trail of all account transactions.
 * 
 * Immutable after creation. Uses idempotency key to guarantee exactly-once processing.
 * Once a transaction is persisted, it cannot be modified.
 */
@Entity
@Table(name = "transaction_logs", indexes = {
        @Index(name = "idx_account_id", columnList = "account_id"),
        @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true)
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "account_id")
    private Long accountId;

    @Column(nullable = false, unique = true, name = "idempotency_key", length = 36)
    private String idempotencyKey;

    @Column(nullable = false)
    private String transactionType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal balanceBefore;

    @Column(nullable = false)
    private BigDecimal balanceAfter;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String description;

    @Column(name = "related_transaction_id")
    private Long relatedTransactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        // Ensure idempotency key is set
        if (idempotencyKey == null) {
            idempotencyKey = UUID.randomUUID().toString();
        }
    }

    // Getters (no setters - immutable after construction)
    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public Long getRelatedTransactionId() {
        return relatedTransactionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
