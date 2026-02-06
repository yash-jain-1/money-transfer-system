package com.moneytransfer.controller;

import com.moneytransfer.dto.request.ForgotPasswordRequest;
import com.moneytransfer.dto.request.ResetPasswordRequest;
import com.moneytransfer.dto.request.UserRegistrationRequest;
import com.moneytransfer.dto.response.UserResponse;
import com.moneytransfer.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for user management operations.
 * Handles user registration, password reset, and profile management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user registration and profile management")
public class UserController {

    private final UserService userService;

    /**
     * Register a new user (ADMIN ONLY).
     * Only administrators can create new user accounts.
     *
     * @param request the registration request
     * @return ResponseEntity with created user information
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Register a new user (Admin Only)", 
        description = "Creates a new user account with USER role. Requires ADMIN role.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("User registration request received for username: {}", request.getUsername());
        
        try {
            UserResponse response = userService.registerUser(request);
            log.info("User registered successfully: {}", response.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("User registration failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Initiate password reset process.
     * Generates a reset token and returns it (in production, would be emailed).
     *
     * @param request the forgot password request
     * @return ResponseEntity with success message and token
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Generates a password reset token for the user")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Password reset requested for email: {}", request.getEmail());
        
        try {
            String message = userService.initiatePasswordReset(request);
            Map<String, String> response = new HashMap<>();
            response.put("message", message);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Password reset initiation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Reset password using reset token.
     *
     * @param request the reset password request
     * @return ResponseEntity with success message
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets user password using a valid reset token")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset attempt with token");
        
        try {
            String message = userService.resetPassword(request);
            Map<String, String> response = new HashMap<>();
            response.put("message", message);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Password reset failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get current user's profile information.
     * Requires authentication.
     *
     * @param username the username from path variable
     * @return ResponseEntity with user information
     */
    @GetMapping("/{username}")
    @Operation(summary = "Get user profile", description = "Retrieves user profile information (requires authentication)")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable String username) {
        log.info("User profile requested for: {}", username);
        
        try {
            UserResponse response = userService.getUserByUsername(username);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("User profile retrieval failed: {}", e.getMessage());
            throw e;
        }
    }
}
