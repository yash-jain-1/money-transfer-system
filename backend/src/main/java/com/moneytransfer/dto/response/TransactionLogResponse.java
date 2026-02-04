package com.moneytransfer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for transaction log responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLogResponse {

    private Long id;

    private Long accountId;

    private String idempotencyKey;

    private String transactionType;

    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private String status;

    private String description;

    private Long relatedTransactionId;

    private LocalDateTime createdAt;
}
