package com.moneytransfer.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User entity representing system users (both regular users and admins).
 * 
 * Each user can own multiple accounts (one-to-many relationship).
 * Users have roles (USER or ADMIN) that determine their access level.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt encoded

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // Password reset fields
    @Column(name = "reset_token", length = 100)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    /**
     * One user can own multiple accounts.
     * Uses lazy loading to avoid loading all accounts when fetching user.
     * mappedBy indicates that Account entity owns the relationship.
     */
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Account> accounts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (enabled == null) {
            enabled = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if the user has ADMIN role.
     *
     * @return true if user is an admin
     */
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(this.role);
    }

    /**
     * Checks if the user has USER role.
     *
     * @return true if user is a regular user
     */
    public boolean isRegularUser() {
        return UserRole.USER.equals(this.role);
    }

    /**
     * Adds an account to this user's account list.
     * Also sets the owner on the account to maintain bidirectional relationship.
     *
     * @param account the account to add
     */
    public void addAccount(Account account) {
        accounts.add(account);
        account.setOwner(this);
    }

    /**
     * Removes an account from this user's account list.
     * Also removes the owner reference from the account.
     *
     * @param account the account to remove
     */
    public void removeAccount(Account account) {
        accounts.remove(account);
        account.setOwner(null);
    }

    /**
     * Checks if this user owns the specified account.
     *
     * @param accountId the account ID to check
     * @return true if the user owns the account
     */
    public boolean ownsAccount(Long accountId) {
        return accounts.stream()
                .anyMatch(account -> account.getId().equals(accountId));
    }
}
