package com.moneytransfer.repository;

import com.moneytransfer.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Repository for Account entity.
 * 
 * Provides data access methods with proper locking strategies:
 * - findById: Uses optimistic locking (JPA default)
 * - findByIdWithPessimisticLock: Uses pessimistic locking for atomic updates
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find account by account number (unique constraint).
     * Useful for querying by human-readable identifier.
     *
     * @param accountNumber the unique account number
     * @return Optional containing the account if found
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Find account by ID with pessimistic write lock.
     * Used during critical sections where we want to guarantee exclusive access.
     * 
     * Prevents other transactions from reading or modifying this account
     * until the current transaction completes.
     *
     * @param id the account ID
     * @return Optional containing the locked account if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithPessimisticLock(@Param("id") Long id);

    /**
     * Check if an account exists by account number.
     * Lightweight check without loading the full entity.
     *
     * @param accountNumber the unique account number
     * @return true if account exists, false otherwise
     */
    boolean existsByAccountNumber(String accountNumber);
}
