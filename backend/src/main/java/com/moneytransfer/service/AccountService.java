package com.moneytransfer.service;

import com.moneytransfer.domain.entity.Account;
import com.moneytransfer.domain.entity.TransactionLog;
import com.moneytransfer.domain.exception.AccountNotFoundException;
import com.moneytransfer.dto.response.AccountBalanceResponse;
import com.moneytransfer.dto.response.AccountResponse;
import com.moneytransfer.dto.response.TransactionLogResponse;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AccountService: Read-only operations for account data.
 * 
 * Methods for USER role:
 * - Regular methods perform ownership checks
 * - Users can only access their own account data
 * 
 * Methods for ADMIN role:
 * - Admin methods bypass ownership checks
 * - Admins can access any account data
 * - Named with "Admin" suffix for clarity
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final OwnershipService ownershipService;

    /**
     * Get account details by ID.
     * Validates user ownership before returning data.
     *
     * @param accountId account ID
     * @return AccountResponse
     */
    public AccountResponse getAccountById(Long accountId) {
        // Validate ownership (admins bypass this check)
        ownershipService.validateAccountOwnership(accountId);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        log.debug("User accessed their account: {}", accountId);
        return toAccountResponse(account);
    }

    /**
     * Get account balance by ID.
     * Validates user ownership before returning balance.
     *
     * @param accountId account ID
     * @return AccountBalanceResponse
     */
    public AccountBalanceResponse getAccountBalance(Long accountId) {
        // Validate ownership (admins bypass this check)
        ownershipService.validateAccountOwnership(accountId);
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        log.debug("User accessed their account balance: {}", accountId);
        return toAccountBalanceResponse(account);
    }

    /**
     * Get transaction history for an account.
     * Validates user ownership before returning history.
     *
     * @param accountId account ID
     * @return list of transaction logs
     */
    public List<TransactionLogResponse> getAccountTransactionHistory(Long accountId) {
        // Validate ownership (admins bypass this check)
        ownershipService.validateAccountOwnership(accountId);
        
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        log.debug("Retrieving transaction history for account: {}", accountId);
        List<TransactionLog> logs = transactionLogRepository.findByFromAccountIdOrderByCreatedAtDesc(accountId);
        return logs.stream()
                .map(this::toTransactionLogResponse)
                .collect(Collectors.toList());
    }
    
    // ========================================
    // ADMIN METHODS - No ownership checks
    // ========================================
    
    /**
     * [ADMIN] Get account details - no ownership check.
     * Admins can view any account.
     *
     * @param accountId account ID
     * @return AccountResponse
     */
    public AccountResponse getAccountAdmin(Long accountId) {
        log.info("[ADMIN ACCESS] Retrieving account details for account: {}", accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return toAccountResponse(account);
    }

    /**
     * [ADMIN] Get account balance - no ownership check.
     * Admins can view any account balance.
     *
     * @param accountId account ID
     * @return AccountBalanceResponse
     */
    public AccountBalanceResponse getAccountBalanceAdmin(Long accountId) {
        log.info("[ADMIN ACCESS] Retrieving balance for account: {}", accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return toAccountBalanceResponse(account);
    }

    /**
     * [ADMIN] Get transaction history - no ownership check.
     * Admins can view any account's transaction history.
     *
     * @param accountId account ID
     * @return list of transaction logs
     */
    public List<TransactionLogResponse> getTransactionHistoryAdmin(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        log.info("[ADMIN ACCESS] Retrieving transaction history for account: {}", accountId);
        List<TransactionLog> logs = transactionLogRepository.findByFromAccountIdOrderByCreatedAtDesc(accountId);
        return logs.stream()
                .map(this::toTransactionLogResponse)
                .collect(Collectors.toList());
    }

    private AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountHolder(account.getAccountHolder())
                .balance(account.getBalance())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private AccountBalanceResponse toAccountBalanceResponse(Account account) {
        return AccountBalanceResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .status(account.getStatus())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private TransactionLogResponse toTransactionLogResponse(TransactionLog logEntry) {
        return TransactionLogResponse.builder()
                .id(logEntry.getId())
                .fromAccountId(logEntry.getFromAccountId())
                .idempotencyKey(logEntry.getIdempotencyKey())
                .transactionType(logEntry.getTransactionType())
                .amount(logEntry.getAmount())
                .balanceBefore(logEntry.getBalanceBefore())
                .balanceAfter(logEntry.getBalanceAfter())
                .status(logEntry.getStatus())
                .description(logEntry.getDescription())
                .toAccountId(logEntry.getToAccountId())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }
}
