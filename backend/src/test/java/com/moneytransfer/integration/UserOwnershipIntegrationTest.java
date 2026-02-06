package com.moneytransfer.integration;

import com.moneytransfer.domain.entity.Account;
import com.moneytransfer.domain.entity.User;
import com.moneytransfer.domain.entity.UserRole;
import com.moneytransfer.dto.request.UserRegistrationRequest;
import com.moneytransfer.dto.response.UserResponse;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.UserRepository;
import com.moneytransfer.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for user registration and account ownership.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("User and Ownership Integration Tests")
class UserOwnershipIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("User registration creates new user with USER role")
    void userRegistration_CreatesNewUser() {
        // Given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .username("newuser")
                .password("password123")
                .email("newuser@example.com")
                .fullName("New User")
                .build();

        // When
        UserResponse response = userService.registerUser(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getEmail()).isEqualTo("newuser@example.com");
        assertThat(response.getFullName()).isEqualTo("New User");
        assertThat(response.getRole()).isEqualTo(UserRole.USER);
        assertThat(response.getEnabled()).isTrue();

        // Verify user exists in database
        User savedUser = userRepository.findByUsername("newuser").orElseThrow();
        assertThat(savedUser.getPassword()).isNotEqualTo("password123"); // Should be encoded
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Cannot register with duplicate username")
    void userRegistration_DuplicateUsername_ThrowsException() {
        // Given
        UserRegistrationRequest firstRequest = UserRegistrationRequest.builder()
                .username("duplicate")
                .password("password123")
                .email("first@example.com")
                .fullName("First User")
                .build();

        userService.registerUser(firstRequest);

        UserRegistrationRequest duplicateRequest = UserRegistrationRequest.builder()
                .username("duplicate")
                .password("password456")
                .email("second@example.com")
                .fullName("Second User")
                .build();

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(duplicateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    @DisplayName("Cannot register with duplicate email")
    void userRegistration_DuplicateEmail_ThrowsException() {
        // Given
        UserRegistrationRequest firstRequest = UserRegistrationRequest.builder()
                .username("user1")
                .password("password123")
                .email("duplicate@example.com")
                .fullName("First User")
                .build();

        userService.registerUser(firstRequest);

        UserRegistrationRequest duplicateRequest = UserRegistrationRequest.builder()
                .username("user2")
                .password("password456")
                .email("duplicate@example.com")
                .fullName("Second User")
                .build();

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(duplicateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("User can own multiple accounts")
    void user_CanOwnMultipleAccounts() {
        // Given
        User user = User.builder()
                .username("multiaccountuser")
                .password(passwordEncoder.encode("password"))
                .email("multi@example.com")
                .fullName("Multi Account User")
                .role(UserRole.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        Account account1 = Account.builder()
                .accountNumber("ACC001")
                .accountHolder("Multi Account User")
                .balance(BigDecimal.valueOf(1000))
                .accountType("SAVINGS")
                .status("ACTIVE")
                .owner(user)
                .build();

        Account account2 = Account.builder()
                .accountNumber("ACC002")
                .accountHolder("Multi Account User")
                .balance(BigDecimal.valueOf(2000))
                .accountType("CHECKING")
                .status("ACTIVE")
                .owner(user)
                .build();

        // When
        user.addAccount(account1);
        user.addAccount(account2);
        userRepository.save(user);

        // Then
        User savedUser = userRepository.findByUsernameWithAccounts("multiaccountuser").orElseThrow();
        assertThat(savedUser.getAccounts()).hasSize(2);
        assertThat(savedUser.getAccounts())
                .extracting(Account::getAccountNumber)
                .containsExactlyInAnyOrder("ACC001", "ACC002");
    }

    @Test
    @DisplayName("Account can be linked to user via addAccount method")
    void account_CanBeLinkedToUser() {
        // Given
        User user = User.builder()
                                .username("linkuser")
                .password(passwordEncoder.encode("password"))
                                .email("linkuser@example.com")
                                .fullName("Link User")
                .role(UserRole.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        Account account = Account.builder()
                .accountNumber("ACC123")
                .accountHolder("Test User")
                .balance(BigDecimal.valueOf(500))
                .accountType("CHECKING")
                .status("ACTIVE")
                .build();

        // When
        user.addAccount(account);
        userRepository.save(user);

        // Then
        Account savedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(savedAccount.getOwner()).isNotNull();
                assertThat(savedAccount.getOwner().getUsername()).isEqualTo("linkuser");
    }

    @Test
    @DisplayName("User.ownsAccount() returns true for owned account")
    void userOwnsAccount_ReturnsTrue() {
        // Given
        User user = User.builder()
                .username("owner")
                .password(passwordEncoder.encode("password"))
                .email("owner@example.com")
                .fullName("Owner")
                .role(UserRole.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        Account account = Account.builder()
                .accountNumber("OWNED")
                .accountHolder("Owner")
                .balance(BigDecimal.valueOf(1000))
                .accountType("CHECKING")
                .status("ACTIVE")
                .owner(user)
                .build();

        user.addAccount(account);
        userRepository.save(user);

        // When
        User savedUser = userRepository.findByUsernameWithAccounts("owner").orElseThrow();
        boolean owns = savedUser.ownsAccount(account.getId());

        // Then
        assertThat(owns).isTrue();
    }

    @Test
    @DisplayName("User.ownsAccount() returns false for non-owned account")
    void userOwnsAccount_ReturnsFalse() {
        // Given
        User user1 = User.builder()
                .username("user1")
                .password(passwordEncoder.encode("password"))
                .email("user1@example.com")
                .fullName("User One")
                .role(UserRole.USER)
                .enabled(true)
                .build();

        User user2 = User.builder()
                .username("user2")
                .password(passwordEncoder.encode("password"))
                .email("user2@example.com")
                .fullName("User Two")
                .role(UserRole.USER)
                .enabled(true)
                .build();

        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);

        Account user2Account = Account.builder()
                .accountNumber("USER2ACC")
                .accountHolder("User Two")
                .balance(BigDecimal.valueOf(1000))
                .accountType("CHECKING")
                .status("ACTIVE")
                .owner(user2)
                .build();

        user2Account = accountRepository.save(user2Account);

        // When
        User savedUser1 = userRepository.findByUsernameWithAccounts("user1").orElseThrow();
        boolean owns = savedUser1.ownsAccount(user2Account.getId());

        // Then
        assertThat(owns).isFalse();
    }

    @Test
    @DisplayName("Admin user can be created with ADMIN role")
    void createAdminUser_CreatesWithAdminRole() {
        // When
        UserResponse response = userService.createAdminUser(
                "admin",
                "adminpass",
                "admin@example.com",
                "Admin User"
        );

        // Then
        assertThat(response.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(response.getUsername()).isEqualTo("admin");

        User savedAdmin = userRepository.findByUsername("admin").orElseThrow();
        assertThat(savedAdmin.isAdmin()).isTrue();
        assertThat(savedAdmin.isRegularUser()).isFalse();
    }

    @Test
    @DisplayName("Regular user has USER role and correct role checks")
    void regularUser_HasCorrectRoleChecks() {
        // Given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .username("regular")
                .password("password123")
                .email("regular@example.com")
                .fullName("Regular User")
                .build();

        userService.registerUser(request);

        // When
        User user = userRepository.findByUsername("regular").orElseThrow();

        // Then
        assertThat(user.isAdmin()).isFalse();
        assertThat(user.isRegularUser()).isTrue();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
    }
}
