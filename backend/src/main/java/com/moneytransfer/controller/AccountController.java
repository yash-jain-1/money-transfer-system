package com.moneytransfer.controller;

import com.moneytransfer.dto.response.AccountBalanceResponse;
import com.moneytransfer.dto.response.AccountResponse;
import com.moneytransfer.dto.response.TransactionLogResponse;
import com.moneytransfer.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AccountController: Read-only APIs for account data.
 *
 * Exposes endpoints for:
 * - GET /accounts/{accountId}: Account details
 * - GET /accounts/{accountId}/balance: Current balance
 * - GET /accounts/{accountId}/transactions: Transaction history
 */
@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Get account details by ID.
     *
     * @param accountId account ID
     * @return account details
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long accountId) {
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
    public ResponseEntity<AccountBalanceResponse> getBalance(@PathVariable Long accountId) {
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
    public ResponseEntity<List<TransactionLogResponse>> getTransactions(@PathVariable Long accountId) {
        log.debug("Fetching transaction history for account: {}", accountId);
        return ResponseEntity.ok(accountService.getAccountTransactionHistory(accountId));
    }
}
