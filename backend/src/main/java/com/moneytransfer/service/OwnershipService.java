package com.moneytransfer.service;

import com.moneytransfer.domain.entity.Account;
import com.moneytransfer.domain.entity.User;
import com.moneytransfer.domain.exception.AccountNotFoundException;
import com.moneytransfer.domain.exception.UnauthorizedAccessException;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for validating user ownership of accounts.
 * Ensures users can only access accounts they own, unless they're admins.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OwnershipService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    /**
     * Validates that the current authenticated user owns the specified account.
     * Admins bypass ownership checks.
     *
     * @param accountId the account ID to validate
     * @throws UnauthorizedAccessException if user doesn't own the account
     * @throws AccountNotFoundException if account doesn't exist
     */
    @Transactional(readOnly = true)
    public void validateAccountOwnership(Long accountId) {
        String username = getCurrentUsername();
        
        // Get user with role information
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found: " + username));

        // Admins can access any account
        if (user.isAdmin()) {
            log.debug("Admin user {} accessing account {}", username, accountId);
            return;
        }

        // Check if account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // Check if user owns the account
        if (account.getOwner() == null || !account.getOwner().getId().equals(user.getId())) {
            log.warn("User {} attempted to access account {} without ownership", username, accountId);
            throw new UnauthorizedAccessException(
                    "You don't have permission to access account: " + accountId);
        }

        log.debug("User {} verified as owner of account {}", username, accountId);
    }

    /**
     * Validates that the current authenticated user owns both accounts.
     * Admins bypass ownership checks.
     *
     * @param fromAccountId the source account ID
     * @param toAccountId the destination account ID
     * @throws UnauthorizedAccessException if user doesn't own the source account
     * @throws AccountNotFoundException if either account doesn't exist
     */
    @Transactional(readOnly = true)
    public void validateTransferOwnership(Long fromAccountId, Long toAccountId) {
        String username = getCurrentUsername();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found: " + username));

        // Admins can transfer between any accounts
        if (user.isAdmin()) {
            log.debug("Admin user {} performing transfer from {} to {}", username, fromAccountId, toAccountId);
            return;
        }

        // Check source account ownership
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException(fromAccountId));

        if (fromAccount.getOwner() == null || !fromAccount.getOwner().getId().equals(user.getId())) {
            log.warn("User {} attempted to transfer from account {} without ownership", username, fromAccountId);
            throw new UnauthorizedAccessException(
                    "You don't have permission to transfer from account: " + fromAccountId);
        }

        // Verify destination account exists (don't need to own it)
        if (!accountRepository.existsById(toAccountId)) {
            throw new AccountNotFoundException(toAccountId);
        }

        log.debug("User {} verified as owner for transfer from {} to {}", username, fromAccountId, toAccountId);
    }

    /**
     * Checks if the current user is an admin.
     *
     * @return true if current user has admin role
     */
    public boolean isCurrentUserAdmin() {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username)
                .map(User::isAdmin)
                .orElse(false);
    }

    /**
     * Checks if the current user owns the specified account.
     *
     * @param accountId the account ID to check
     * @return true if user owns the account or is an admin
     */
    @Transactional(readOnly = true)
    public boolean currentUserOwnsAccount(Long accountId) {
        try {
            validateAccountOwnership(accountId);
            return true;
        } catch (UnauthorizedAccessException e) {
            return false;
        }
    }

    /**
     * Gets the current authenticated user.
     *
     * @return the current user
     * @throws UnauthorizedAccessException if user is not authenticated or not found
     */
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found: " + username));
    }

    /**
     * Gets the current authenticated username from Spring Security context.
     *
     * @return the username
     * @throws UnauthorizedAccessException if no authentication found
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("No authenticated user found");
        }

        String username = authentication.getName();
        if (username == null || username.equals("anonymousUser")) {
            throw new UnauthorizedAccessException("Anonymous access not allowed");
        }

        return username;
    }
}
