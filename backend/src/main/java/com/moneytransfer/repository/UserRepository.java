package com.moneytransfer.repository;

import com.moneytransfer.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity.
 * Provides database access methods for user management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by username.
     *
     * @param username the username
     * @return Optional containing user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by email.
     *
     * @param email the email address
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by reset token.
     *
     * @param resetToken the password reset token
     * @return Optional containing user if found
     */
    Optional<User> findByResetToken(String resetToken);

    /**
     * Check if username already exists.
     *
     * @param username the username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email already exists.
     *
     * @param email the email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find a user by username with their accounts eagerly loaded.
     * Useful for checking account ownership.
     *
     * @param username the username
     * @return Optional containing user with accounts
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.accounts WHERE u.username = :username")
    Optional<User> findByUsernameWithAccounts(@Param("username") String username);
}
