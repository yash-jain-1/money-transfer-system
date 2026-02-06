package com.moneytransfer.service;

import com.moneytransfer.domain.entity.Account;
import com.moneytransfer.domain.entity.User;
import com.moneytransfer.domain.entity.UserRole;
import com.moneytransfer.domain.exception.AccountNotFoundException;
import com.moneytransfer.domain.exception.UnauthorizedAccessException;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for OwnershipService - ownership validation logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OwnershipService Tests")
class OwnershipServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private OwnershipService ownershipService;

    private User regularUser;
    private User adminUser;
    private Account userOwnedAccount;
    private Account otherAccount;

    @BeforeEach
    void setUp() {
        // Setup test data
        regularUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(UserRole.USER)
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("admin")
                .role(UserRole.ADMIN)
                .build();

        userOwnedAccount = Account.builder()
                .id(100L)
                .accountNumber("ACC100")
                .accountHolder("Test User")
                .balance(BigDecimal.valueOf(1000))
                .owner(regularUser)
                .build();

        otherAccount = Account.builder()
                .id(200L)
                .accountNumber("ACC200")
                .accountHolder("Other User")
                .balance(BigDecimal.valueOf(2000))
                .owner(User.builder().id(3L).username("other").build())
                .build();

        // Setup security context
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
    }

    @Test
    @DisplayName("Regular user can access their own account")
    void regularUserCanAccessOwnAccount() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(userOwnedAccount));

        // When & Then
        assertThatCode(() -> ownershipService.validateAccountOwnership(100L))
                .doesNotThrowAnyException();

        verify(userRepository).findByUsername("testuser");
        verify(accountRepository).findById(100L);
    }

    @Test
    @DisplayName("Regular user cannot access another user's account")
    void regularUserCannotAccessOtherAccount() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));
        when(accountRepository.findById(200L)).thenReturn(Optional.of(otherAccount));

        // When & Then
        assertThatThrownBy(() -> ownershipService.validateAccountOwnership(200L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("You don't have permission to access account: 200");
    }

    @Test
    @DisplayName("Admin user can access any account")
    void adminCanAccessAnyAccount() {
        // Given
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // When & Then
        assertThatCode(() -> ownershipService.validateAccountOwnership(200L))
                .doesNotThrowAnyException();

        verify(userRepository).findByUsername("admin");
        // Admin bypass - no need to check account
    }

    @Test
    @DisplayName("Throw exception when account not found")
    void throwExceptionWhenAccountNotFound() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> ownershipService.validateAccountOwnership(999L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("Throw exception when user not found")
    void throwExceptionWhenUserNotFound() {
        // Given
        when(authentication.getName()).thenReturn("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> ownershipService.validateAccountOwnership(100L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("User not found: unknown");
    }

    @Test
    @DisplayName("Regular user can transfer from their own account")
    void regularUserCanTransferFromOwnAccount() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(userOwnedAccount));
        when(accountRepository.existsById(200L)).thenReturn(true);

        // When & Then
        assertThatCode(() -> ownershipService.validateTransferOwnership(100L, 200L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Regular user cannot transfer from another user's account")
    void regularUserCannotTransferFromOtherAccount() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));
        when(accountRepository.findById(200L)).thenReturn(Optional.of(otherAccount));

        // When & Then
        assertThatThrownBy(() -> ownershipService.validateTransferOwnership(200L, 100L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("You don't have permission to transfer from account: 200");
    }

    @Test
    @DisplayName("Admin can transfer between any accounts")
    void adminCanTransferBetweenAnyAccounts() {
        // Given
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // When & Then
        assertThatCode(() -> ownershipService.validateTransferOwnership(100L, 200L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Check if current user is admin - returns true for admin")
    void checkIfCurrentUserIsAdmin_AdminUser() {
        // Given
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // When
        boolean isAdmin = ownershipService.isCurrentUserAdmin();

        // Then
        assertThat(isAdmin).isTrue();
    }

    @Test
    @DisplayName("Check if current user is admin - returns false for regular user")
    void checkIfCurrentUserIsAdmin_RegularUser() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));

        // When
        boolean isAdmin = ownershipService.isCurrentUserAdmin();

        // Then
        assertThat(isAdmin).isFalse();
    }

    @Test
    @DisplayName("Current user owns account - returns true")
    void currentUserOwnsAccount_ReturnsTrue() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(userOwnedAccount));

        // When
        boolean owns = ownershipService.currentUserOwnsAccount(100L);

        // Then
        assertThat(owns).isTrue();
    }

    @Test
    @DisplayName("Current user owns account - returns false")
    void currentUserOwnsAccount_ReturnsFalse() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));
        when(accountRepository.findById(200L)).thenReturn(Optional.of(otherAccount));

        // When
        boolean owns = ownershipService.currentUserOwnsAccount(200L);

        // Then
        assertThat(owns).isFalse();
    }

    @Test
    @DisplayName("Get current user - returns user")
    void getCurrentUser_ReturnsUser() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));

        // When
        User currentUser = ownershipService.getCurrentUser();

        // Then
        assertThat(currentUser).isNotNull();
        assertThat(currentUser.getUsername()).isEqualTo("testuser");
        assertThat(currentUser.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("Throw exception when no authentication context")
    void throwExceptionWhenNoAuthentication() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> ownershipService.validateAccountOwnership(100L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    @DisplayName("Throw exception for anonymous user")
    void throwExceptionForAnonymousUser() {
        // Given
        when(authentication.getName()).thenReturn("anonymousUser");

        // When & Then
        assertThatThrownBy(() -> ownershipService.validateAccountOwnership(100L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("Anonymous access not allowed");
    }
}
