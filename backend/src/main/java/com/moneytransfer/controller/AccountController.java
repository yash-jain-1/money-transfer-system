package com.moneytransfer.controller;

import com.moneytransfer.dto.response.AccountBalanceResponse;
import com.moneytransfer.dto.response.AccountResponse;
import com.moneytransfer.dto.response.TransactionLogResponse;
import com.moneytransfer.service.AccountService;
import com.moneytransfer.util.RateLimitUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AccountController: Read-only APIs for account data with rate limiting.
 *
 * Exposes endpoints for:
 * - GET /accounts/{accountId}: Account details
 * - GET /accounts/{accountId}/balance: Current balance
 * - GET /accounts/{accountId}/transactions: Transaction history
 * 
 * Rate Limit: 60 reads per minute per user
 */
@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account information and history")
public class AccountController {

    private final AccountService accountService;
    private final RateLimitUtil rateLimitUtil;

    /**
     * Get account details by ID.
     *
     * @param accountId account ID
     * @return account details
     */
    @GetMapping("/{accountId}")
    @Operation(summary = "Get account details", description = "Retrieve account information by ID")
    @ApiResponse(responseCode = "200", description = "Account found")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded - max 60 reads per minute")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long accountId) {
        // Rate limit: 60 account reads per minute per user
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!rateLimitUtil.allowAccountRead(userId)) {
            log.warn("Account read rate limit exceeded for user: {}", userId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        log.debug("Fetching account details for account: {}", accountId);
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    /**
     * Get current account balance by ID.
     *
     * @param accountId account ID
     * @return current account balance
     */
    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get account balance", description = "Retrieve current balance for an account")
    @ApiResponse(responseCode = "200", description = "Balance retrieved")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded - max 60 reads per minute")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<AccountBalanceResponse> getBalance(@PathVariable Long accountId) {
        // Rate limit: 60 account reads per minute per user
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!rateLimitUtil.allowAccountRead(userId)) {
            log.warn("Account read rate limit exceeded for user: {}", userId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        log.debug("Fetching account balance for account: {}", accountId);
        return ResponseEntity.ok(accountService.getAccountBalance(accountId));
    }

    /**
     * Get transaction history for an account.
     *
     * @param accountId account ID
     * @return list of transactions for the account
     */
    @GetMapping("/{accountId}/transactions")
    @Operation(summary = "Get transaction history", description = "Retrieve all transactions for an account")
    @ApiResponse(responseCode = "200", description = "Transactions retrieved")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded - max 60 reads per minute")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<List<TransactionLogResponse>> getTransactions(@PathVariable Long accountId) {
        // Rate limit: 60 account reads per minute per user
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!rateLimitUtil.allowAccountRead(userId)) {
            log.warn("Account read rate limit exceeded for user: {}", userId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        log.debug("Fetching transaction history for account: {}", accountId);
        return ResponseEntity.ok(accountService.getAccountTransactionHistory(accountId));
    }
}
