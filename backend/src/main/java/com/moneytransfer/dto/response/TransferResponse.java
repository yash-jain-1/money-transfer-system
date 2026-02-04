package com.moneytransfer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for transfer responses.
 * Output for transfer API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {

    private Long transactionId;

    private Long sourceAccountId;

    private Long destinationAccountId;

    private BigDecimal amount;

    private String transactionType;

    private String status;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
