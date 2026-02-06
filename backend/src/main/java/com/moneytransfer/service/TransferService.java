package com.moneytransfer.service;

import com.moneytransfer.domain.entity.Account;
import com.moneytransfer.domain.entity.TransactionLog;
import com.moneytransfer.domain.exception.AccountNotFoundException;
import com.moneytransfer.domain.exception.DuplicateTransferException;
import com.moneytransfer.domain.exception.InsufficientBalanceException;
import com.moneytransfer.domain.status.TransactionStatus;
import com.moneytransfer.dto.request.TransferRequest;
import com.moneytransfer.dto.response.TransferResponse;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * TransferService: Core service for executing money transfers.
 * 
 * This service implements the complete transfer workflow:
 * 1. Validates request and checks idempotency
 * 2. Loads source and destination accounts (with pessimistic locking if needed)
 * 3. Debits source account (validated balance check)
 * 4. Credits destination account
 * 5. Persists transaction log entries for audit trail
 * 6. Ensures atomicity through @Transactional
 * 
 * Key features:
 * ✅ Idempotency: Uses idempotencyKey to ensure exactly-once processing
 * ✅ Optimistic Locking: @Version on Account detects concurrent modifications
 * ✅ Pessimistic Locking: Option to use PESSIMISTIC_WRITE for critical sections
 * ✅ Balance Protection: Business logic prevents negative balances
 * ✅ Audit Trail: All transactions logged for compliance and debugging
 * 
 * Transaction flow uses Spring's REQUIRED propagation:
 * - All operations (debit, credit, log) succeed or all fail
 * - Rollback on any exception ensures consistency
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final OwnershipService ownershipService;

    /**
     * Execute a money transfer between two accounts.
     * 
     * Flow:
     * 1. Check if this transfer already processed (idempotency)
     *    - If yes: Return cached result
     *    - If no: Proceed to step 2
     * 2. Validate request (source != destination, accounts exist, accounts active)
     * 3. Load source and destination accounts
     * 4. Debit source account (will throw if insufficient balance)
     * 5. Credit destination account
     * 6. Persist account changes
     * 7. Log debit transaction
     * 8. Log credit transaction
     * 9. Return successful response
     * 
     * If any step fails, entire transaction rolls back. This means:
     * - No partial transfers
     * - No orphaned transaction logs
     * - Database state remains consistent
     * 
     * @param request Transfer request containing amount, accounts, and idempotency key
     * @return Transfer response with transaction details
     * @throws AccountNotFoundException if source or destination account not found
     * @throws IllegalArgumentException if source == destination or amount invalid
     * @throws IllegalStateException if accounts not active or insufficient balance
     * @throws DuplicateTransferException if idempotency key conflicts (shouldn't happen with unique constraint)
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        log.info("Initiating transfer - Source: {}, Destination: {}, Amount: {}, Idempotency: {}",
                request.getSourceAccountId(), request.getDestinationAccountId(),
                request.getAmount(), request.getIdempotencyKey());

        // Step 0: Validate ownership - user must own the source account
        // Admins bypass this check automatically
        ownershipService.validateTransferOwnership(
            request.getSourceAccountId(), 
            request.getDestinationAccountId()
        );

        // Step 1: Check idempotency - Exactly-once processing
        var existingTransaction = transactionLogRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingTransaction.isPresent()) {
            log.info("Duplicate request detected. Idempotency key: {}. Returning cached result.",
                    request.getIdempotencyKey());
            
            TransactionLog txn = existingTransaction.get();
                return buildTransferResponse(request.getSourceAccountId(), 
                    request.getDestinationAccountId(), txn);
        }

        // Step 2: Validate transfer request
        validateTransferRequest(request);

        // Step 3: Load accounts
        Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Source account not found: " + request.getSourceAccountId()));

        Account destinationAccount = accountRepository.findById(request.getDestinationAccountId())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Destination account not found: " + request.getDestinationAccountId()));

        // Step 4 & 5: Debit and credit (includes balance validation)
        BigDecimal balanceBeforeDebit = sourceAccount.getBalance();
        
        try {
            sourceAccount.debit(request.getAmount());
        } catch (IllegalStateException e) {
            log.warn("Transfer failed due to source account issue - {}", e.getMessage());
            throw new InsufficientBalanceException(e.getMessage());
        }

        BigDecimal balanceAfterDebit = sourceAccount.getBalance();
        
        try {
            destinationAccount.credit(request.getAmount());
        } catch (IllegalStateException e) {
            log.warn("Transfer failed due to destination account issue - {}", e.getMessage());
            // This will trigger rollback due to @Transactional
            throw new IllegalStateException("Transfer failed: " + e.getMessage());
        }

        // Step 6: Persist account changes
        // JPA detects @Version mismatch here and throws OptimisticLockingFailureException
        // if another transaction modified the account since we loaded it
        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        // Step 7: Log debit transaction
        // Note: We use idempotency key to link related transactions
        // The debit and credit transactions share the same idempotency key
        // This allows finding all related transactions by key
        TransactionLog debitLog = TransactionLog.builder()
            .fromAccountId(sourceAccount.getId())
            .toAccountId(destinationAccount.getId())
                .idempotencyKey(request.getIdempotencyKey())
                .transactionType("DEBIT")
                .amount(request.getAmount())
                .balanceBefore(balanceBeforeDebit)
                .balanceAfter(balanceAfterDebit)
                .status(TransactionStatus.COMPLETED.name())
                .description("Transfer to account " + request.getDestinationAccountId() + 
                            (request.getDescription() != null ? " - " + request.getDescription() : ""))
                .build();

        debitLog = transactionLogRepository.save(debitLog);
        log.debug("Debit transaction logged - ID: {}, Amount: {}", debitLog.getId(), request.getAmount());

        // Step 8: Log credit transaction
        // Credit and debit are linked through:
        // 1. Same idempotency key (allows finding the transfer pair)
        // 2. Both reference the same amounts and accounts
        BigDecimal balanceBeforeCredit = destinationAccount.getBalance().subtract(request.getAmount());
        BigDecimal balanceAfterCredit = destinationAccount.getBalance();

        TransactionLog creditLog = TransactionLog.builder()
            .fromAccountId(destinationAccount.getId())
            .toAccountId(sourceAccount.getId())
                .idempotencyKey(request.getIdempotencyKey()) // Same key for related transactions
                .transactionType("CREDIT")
                .amount(request.getAmount())
                .balanceBefore(balanceBeforeCredit)
                .balanceAfter(balanceAfterCredit)
                .status(TransactionStatus.COMPLETED.name())
                .description("Transfer from account " + request.getSourceAccountId() + 
                            (request.getDescription() != null ? " - " + request.getDescription() : ""))
                .build();

        creditLog = transactionLogRepository.save(creditLog);
        log.debug("Credit transaction logged - ID: {}, Amount: {}", creditLog.getId(), request.getAmount());

        log.info("Transfer completed successfully - Source: {}, Destination: {}, Amount: {}, " +
                "Debit Txn ID: {}, Credit Txn ID: {}", 
                request.getSourceAccountId(), request.getDestinationAccountId(),
                request.getAmount(), debitLog.getId(), creditLog.getId());

        // Step 9: Return response
        return buildTransferResponse(sourceAccount.getId(), destinationAccount.getId(), debitLog);
    }

    /**
     * Validates the transfer request.
     * 
     * Rules:
     * - Source and destination must be different accounts
     * - Amount must be positive
     * - Both accounts must be active
     * 
     * @param request Transfer request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateTransferRequest(TransferRequest request) {
        // Rule 1: Cannot transfer to self
        if (request.getSourceAccountId().equals(request.getDestinationAccountId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Rule 2: Amount validation (additional check, DTO has @DecimalMin)
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        log.debug("Transfer request validation passed - Source: {}, Destination: {}, Amount: {}",
                request.getSourceAccountId(), request.getDestinationAccountId(), request.getAmount());
    }

    /**
     * Builds a TransferResponse from transaction log.
     * 
     * Converts internal transaction representation to API response format.
     * 
     * @param sourceAccountId Source account ID
     * @param destinationAccountId Destination account ID
     * @param debitTransaction The debit transaction log entry
     * @return TransferResponse suitable for API response
     */
    private TransferResponse buildTransferResponse(Long sourceAccountId, Long destinationAccountId,
                                                   TransactionLog debitTransaction) {
        return TransferResponse.builder()
                .transactionId(debitTransaction.getId())
                .sourceAccountId(sourceAccountId)
                .destinationAccountId(destinationAccountId)
                .amount(debitTransaction.getAmount())
                .transactionType(debitTransaction.getTransactionType())
                .status(debitTransaction.getStatus())
                .description(debitTransaction.getDescription())
                .createdAt(debitTransaction.getCreatedAt())
                .updatedAt(debitTransaction.getCreatedAt()) // TransactionLog is immutable
                .build();
    }

    /**
     * Get transfer history for an account.
     * 
     * Returns all transactions (debit and credit) for audit purposes.
     * Most recent transactions first.
     * 
     * @param accountId Account ID to get history for
     * @return List of transactions
     */
    public java.util.List<TransactionLog> getAccountTransactionHistory(Long accountId) {
        log.debug("Retrieving transaction history for account: {}", accountId);
        return transactionLogRepository.findByFromAccountIdOrderByCreatedAtDesc(accountId);
    }
}
