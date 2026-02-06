package com.moneytransfer.service;

import com.moneytransfer.domain.entity.User;
import com.moneytransfer.domain.entity.UserRole;
import com.moneytransfer.dto.request.ForgotPasswordRequest;
import com.moneytransfer.dto.request.ResetPasswordRequest;
import com.moneytransfer.dto.request.UserRegistrationRequest;
import com.moneytransfer.dto.response.UserResponse;
import com.moneytransfer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for user management operations.
 * Handles user registration, password reset, and profile management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user in the system.
     * Default role is USER. Admin users must be created differently.
     *
     * @param request the registration request
     * @return UserResponse with created user information
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Attempting to register new user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: Username already exists: {}", request.getUsername());
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Create new user with USER role
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(UserRole.USER) // Default role
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered new user: {}", savedUser.getUsername());

        return toUserResponse(savedUser);
    }

    /**
     * Initiates password reset process by generating a reset token.
     *
     * @param request the forgot password request with email
     * @return success message
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public String initiatePasswordReset(ForgotPasswordRequest request) {
        log.info("Password reset requested for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Password reset failed: User not found with email: {}", request.getEmail());
                    return new IllegalArgumentException("User not found with email: " + request.getEmail());
                });

        // Generate reset token (UUID)
        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(24)); // Token valid for 24 hours

        userRepository.save(user);
        log.info("Password reset token generated for user: {}", user.getUsername());

        // In production, send this token via email
        // For now, return it in the response (NOT RECOMMENDED FOR PRODUCTION)
        return "Password reset token generated. Token: " + resetToken + 
               " (In production, this would be sent via email)";
    }

    /**
     * Resets user password using a valid reset token.
     *
     * @param request the reset password request with token and new password
     * @return success message
     * @throws IllegalArgumentException if token is invalid or expired
     */
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        log.info("Attempting to reset password with token");

        User user = userRepository.findByResetToken(request.getResetToken())
                .orElseThrow(() -> {
                    log.warn("Password reset failed: Invalid reset token");
                    return new IllegalArgumentException("Invalid reset token");
                });

        // Check if token is expired
        if (user.getResetTokenExpiry() == null || 
            LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            log.warn("Password reset failed: Reset token expired for user: {}", user.getUsername());
            throw new IllegalArgumentException("Reset token has expired");
        }

        // Update password and clear reset token
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);
        log.info("Password successfully reset for user: {}", user.getUsername());

        return "Password has been reset successfully";
    }

    /**
     * Gets user information by username.
     *
     * @param username the username
     * @return UserResponse with user information
     * @throws IllegalArgumentException if user not found
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return toUserResponse(user);
    }

    /**
     * Creates an admin user (internal use only).
     * This should be called during application initialization or by system administrators.
     *
     * @param username the admin username
     * @param password the admin password
     * @param email the admin email
     * @param fullName the admin full name
     * @return UserResponse with created admin information
     */
    @Transactional
    public UserResponse createAdminUser(String username, String password, String email, String fullName) {
        log.info("Creating admin user: {}", username);

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        User admin = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .fullName(fullName)
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();

        User savedAdmin = userRepository.save(admin);
        log.info("Successfully created admin user: {}", savedAdmin.getUsername());

        return toUserResponse(savedAdmin);
    }

    /**
     * Converts User entity to UserResponse DTO.
     *
     * @param user the user entity
     * @return UserResponse DTO
     */
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}
