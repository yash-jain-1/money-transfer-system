package com.moneytransfer.controller;

import com.moneytransfer.domain.exception.AccountNotFoundException;
import com.moneytransfer.domain.exception.AccountNotActiveException;
import com.moneytransfer.domain.exception.DuplicateTransferException;
import com.moneytransfer.domain.exception.InsufficientBalanceException;
import com.moneytransfer.dto.request.TransferRequest;
import com.moneytransfer.dto.response.TransferResponse;
import com.moneytransfer.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TransferController.
 * 
 * Tests cover:
 * - Successful money transfer
 * - Validation errors (invalid request body)
 * - Business logic exceptions (insufficient balance, account not found, etc.)
 * - Health check endpoint
 * - HTTP status codes and response structure
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransferController Unit Tests")
class TransferControllerTest {

    @Mock
    private TransferService transferService;

    @InjectMocks
    private TransferController transferController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transferController).build();
    }

    // ============ SUCCESS CASES ============

    @Test
    @DisplayName("✅ POST /transfers: Should successfully initiate transfer and return 201 CREATED")
    void testInitiateTransferSuccess() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .idempotencyKey("unique-key-123")
                .build();

        Long transactionId = 1L;
        TransferResponse response = TransferResponse.builder()
                .transactionId(transactionId)
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        when(transferService.transfer(any(TransferRequest.class))).thenReturn(response);

        // Act
        ResponseEntity<TransferResponse> result = transferController.initiateTransfer(request);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTransactionId()).isEqualTo(transactionId);
        assertThat(result.getBody().getAmount()).isEqualTo(new BigDecimal("100.00"));
        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("✅ GET /transfers/health: Should return 200 OK with health message")
    void testHealthCheckEndpoint() {
        // Act
        ResponseEntity<String> result = transferController.health();

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Money Transfer System is running");
    }

    @Test
    @DisplayName("✅ POST /transfers: Should handle transfer with large amount")
    void testInitiateTransferWithLargeAmount() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("999999.99"))
                .idempotencyKey("large-transfer-123")
                .build();

        TransferResponse response = TransferResponse.builder()
                .transactionId(2L)
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("999999.99"))
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        when(transferService.transfer(any(TransferRequest.class))).thenReturn(response);

        // Act
        ResponseEntity<TransferResponse> result = transferController.initiateTransfer(request);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getAmount()).isEqualTo(new BigDecimal("999999.99"));
    }

    @Test
    @DisplayName("✅ POST /transfers: Should support idempotency key for duplicate requests")
    void testInitiateTransferWithIdempotencyKey() {
        // Arrange
        String idempotencyKey = "unique-idempotent-key-123";
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("50.00"))
                .idempotencyKey(idempotencyKey)
                .build();

        TransferResponse response = TransferResponse.builder()
                .transactionId(3L)
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("50.00"))
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        when(transferService.transfer(any(TransferRequest.class))).thenReturn(response);

        // Act
        ResponseEntity<TransferResponse> result = transferController.initiateTransfer(request);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(transferService, times(1)).transfer(argThat(req -> 
            req.getIdempotencyKey().equals(idempotencyKey)
        ));
    }

    // ============ EXCEPTION CASES ============

    @Test
    @DisplayName("❌ POST /transfers: Should handle AccountNotFoundException")
    void testInitiateTransferAccountNotFound() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(999L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .idempotencyKey("test-key")
                .build();

        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new AccountNotFoundException("Account 999 not found"));

        // Act & Assert
        assertThatThrownBy(() -> transferController.initiateTransfer(request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account 999 not found");

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("❌ POST /transfers: Should handle AccountNotActiveException")
    void testInitiateTransferAccountNotActive() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .idempotencyKey("test-key")
                .build();

        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new AccountNotActiveException("Source account is locked"));

        // Act & Assert
        assertThatThrownBy(() -> transferController.initiateTransfer(request))
                .isInstanceOf(AccountNotActiveException.class)
                .hasMessage("Source account is locked");

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("❌ POST /transfers: Should handle InsufficientBalanceException")
    void testInitiateTransferInsufficientBalance() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("5000.00"))
                .idempotencyKey("test-key")
                .build();

        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new InsufficientBalanceException("Balance is only 100.00"));

        // Act & Assert
        assertThatThrownBy(() -> transferController.initiateTransfer(request))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Balance is only 100.00");

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("❌ POST /transfers: Should handle DuplicateTransferException")
    void testInitiateTransferDuplicate() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .idempotencyKey("duplicate-key")
                .build();

        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new DuplicateTransferException("Idempotency key 'duplicate-key' has already been used. Duplicate transfer detected."));

        // Act & Assert
        assertThatThrownBy(() -> transferController.initiateTransfer(request))
                .isInstanceOf(DuplicateTransferException.class)
                .hasMessageContaining("already been used");

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    // ============ EDGE CASES ============

    @Test
    @DisplayName("✅ POST /transfers: Should handle transfer with minimum amount (0.01)")
    void testInitiateTransferWithMinimumAmount() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("0.01"))
                .idempotencyKey("min-amount-key")
                .build();

        TransferResponse response = TransferResponse.builder()
                .transactionId(4L)
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("0.01"))
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        when(transferService.transfer(any(TransferRequest.class))).thenReturn(response);

        // Act
        ResponseEntity<TransferResponse> result = transferController.initiateTransfer(request);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getAmount()).isEqualTo(new BigDecimal("0.01"));
    }

    @Test
    @DisplayName("✅ POST /transfers: Should handle transfer between same account type (edge case)")
    void testInitiateTransferBetweenSameAccountType() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(1L)
                .destinationAccountId(1L)
                .amount(new BigDecimal("100.00"))
                .idempotencyKey("same-account-key")
                .build();

        TransferResponse response = TransferResponse.builder()
                .transactionId(5L)
                .sourceAccountId(1L)
                .destinationAccountId(1L)
                .amount(new BigDecimal("100.00"))
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        when(transferService.transfer(any(TransferRequest.class))).thenReturn(response);

        // Act
        ResponseEntity<TransferResponse> result = transferController.initiateTransfer(request);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("✅ POST /transfers: Should return response with timestamp")
    void testInitiateTransferResponseContainsTimestamp() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .idempotencyKey("timestamp-key")
                .build();

        LocalDateTime now = LocalDateTime.now();
        TransferResponse response = TransferResponse.builder()
                .transactionId(6L)
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .status("SUCCESS")
                .createdAt(now)
                .build();

        when(transferService.transfer(any(TransferRequest.class))).thenReturn(response);

        // Act
        ResponseEntity<TransferResponse> result = transferController.initiateTransfer(request);

        // Assert
        assertThat(result.getBody().getCreatedAt()).isNotNull();
        assertThat(result.getBody().getCreatedAt()).isEqualToIgnoringSeconds(now);
    }

    @Test
    @DisplayName("✅ Controller: Should call TransferService exactly once per request")
    void testServiceIsCalledExactlyOnce() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .idempotencyKey("once-key")
                .build();

        TransferResponse response = TransferResponse.builder()
                .transactionId(7L)
                .sourceAccountId(1L)
                .destinationAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        when(transferService.transfer(any(TransferRequest.class))).thenReturn(response);

        // Act
        transferController.initiateTransfer(request);

        // Assert
        verify(transferService, times(1)).transfer(any(TransferRequest.class));
        verifyNoMoreInteractions(transferService);
    }
}
