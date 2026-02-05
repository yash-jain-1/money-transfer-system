package com.moneytransfer.controller;

import com.moneytransfer.dto.request.TransferRequest;
import com.moneytransfer.dto.response.TransferResponse;
import com.moneytransfer.service.TransferService;
import com.moneytransfer.util.RateLimitUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * TransferController: REST API endpoint for money transfer operations.
 * 
 * Exposes endpoints for:
 * - POST /transfers: Initiate a money transfer with idempotency support
 * 
 * All endpoints are secured by Spring Security and require authentication.
 */
@Slf4j
@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Money transfer operations")
public class TransferController {

    private final TransferService transferService;
    private final RateLimitUtil rateLimitUtil;

    /**
     * Initiate a money transfer between two accounts.
     * 
     * @param transferRequest Request containing sourceAccountId, destinationAccountId, amount, and idempotencyKey
     * @return TransferResponse with transaction details
     */
    @PostMapping
    @Operation(summary = "Initiate money transfer", description = "Transfer funds between accounts with idempotency support")
    @ApiResponse(responseCode = "201", description = "Transfer initiated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "409", description = "Insufficient funds")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded - max 10 transfers per minute")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<TransferResponse> initiateTransfer(
            @Valid @RequestBody TransferRequest transferRequest) {
        
        // Rate limit: 10 transfers per minute per user
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!rateLimitUtil.allowTransfer(userId)) {
            log.warn("Transfer rate limit exceeded for user: {}", userId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(TransferResponse.builder()
                            .status("RATE_LIMITED")
                            .description("Transfer rate limit exceeded. Maximum 10 transfers per minute.")
                            .build());
        }
        
        log.info("Initiating transfer from account {} to account {} with amount {}",
                transferRequest.getSourceAccountId(),
                transferRequest.getDestinationAccountId(),
                transferRequest.getAmount());
        
        TransferResponse response = transferService.transfer(transferRequest);
        
        log.info("Transfer completed successfully with transaction ID: {}", response.getTransactionId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Health check endpoint.
     * 
     * @return Simple status response
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verify service is running")
    @ApiResponse(responseCode = "200", description = "Service is running")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Money Transfer System is running");
    }
}
