package com.moneytransfer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for account responses.
 * Output for account API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    private Long id;

    private String accountNumber;

    private String accountHolder;

    private BigDecimal balance;

    private String accountType;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
