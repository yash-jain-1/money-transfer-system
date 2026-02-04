package com.moneytransfer.controller;

import com.moneytransfer.dto.request.TransferRequest;
import com.moneytransfer.dto.response.TransferResponse;
import com.moneytransfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class TransferController {

    private final TransferService transferService;

    /**
     * Initiate a money transfer between two accounts.
     * 
     * @param transferRequest Request containing sourceAccountId, destinationAccountId, amount, and idempotencyKey
     * @return TransferResponse with transaction details
     */
    @PostMapping
    public ResponseEntity<TransferResponse> initiateTransfer(
            @Valid @RequestBody TransferRequest transferRequest) {
        
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
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Money Transfer System is running");
    }
}
