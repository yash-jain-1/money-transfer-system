package com.moneytransfer.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for transfer requests.
 * Input for transfer API with validation constraints.
 * 
 * idempotencyKey: Ensures exactly-once processing of transfers.
 * If same request is retried with same key, no duplicate transfer occurs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    @NotNull(message = "Source account ID cannot be null")
    @Positive(message = "Source account ID must be positive")
    private Long sourceAccountId;

    @NotNull(message = "Destination account ID cannot be null")
    @Positive(message = "Destination account ID must be positive")
    private Long destinationAccountId;

    @NotNull(message = "Transfer amount cannot be null")
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Idempotency key cannot be null")
    @Size(min = 36, max = 36, message = "Idempotency key must be a valid UUID")
    private String idempotencyKey;
}
