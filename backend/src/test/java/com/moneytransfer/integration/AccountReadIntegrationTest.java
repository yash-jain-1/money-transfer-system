package com.moneytransfer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneytransfer.domain.entity.Account;
import com.moneytransfer.domain.status.AccountStatus;
import com.moneytransfer.dto.request.TransferRequest;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
class AccountReadIntegrationTest {

        @DynamicPropertySource
        static void registerMySqlProperties(DynamicPropertyRegistry registry) {
                Map<String, String> env = loadDotEnvIfPresent();
                String url = firstNonBlank(System.getenv("DB_URL"), System.getProperty("DB_URL"), env.get("DB_URL"));
                String username = firstNonBlank(System.getenv("DB_USERNAME"), System.getProperty("DB_USERNAME"), env.get("DB_USERNAME"));
                String password = firstNonBlank(System.getenv("DB_PASSWORD"), System.getProperty("DB_PASSWORD"), env.get("DB_PASSWORD"));

                if (url == null || username == null || password == null) {
                        throw new IllegalStateException("DB_URL, DB_USERNAME, and DB_PASSWORD must be set for integration tests.");
                }

                registry.add("spring.datasource.url", () -> url);
                registry.add("spring.datasource.username", () -> username);
                registry.add("spring.datasource.password", () -> password);
                registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
                registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
                registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQL8Dialect");
                registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @BeforeEach
    void setUp() {
        transactionLogRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("✅ GET /accounts/{id}/balance returns current balance")
    @WithMockUser
    void getAccountBalanceReturnsCurrentBalance() throws Exception {
        Account account = accountRepository.save(Account.builder()
                .accountNumber("ACC100")
                .accountHolder("Alice Johnson")
                .balance(new BigDecimal("1000.00"))
                .accountType("CHECKING")
                .status(AccountStatus.ACTIVE.name())
                .build());

        mockMvc.perform(get("/accounts/{accountId}/balance", account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(account.getId()))
                .andExpect(jsonPath("$.accountNumber").value("ACC100"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @DisplayName("✅ GET /accounts/{id}/transactions returns transaction history")
    @WithMockUser
    void getAccountTransactionHistoryReturnsTransactions() throws Exception {
        Account source = accountRepository.save(Account.builder()
                .accountNumber("SRC001")
                .accountHolder("Source User")
                .balance(new BigDecimal("500.00"))
                .accountType("SAVINGS")
                .status(AccountStatus.ACTIVE.name())
                .build());

        Account destination = accountRepository.save(Account.builder()
                .accountNumber("DST001")
                .accountHolder("Destination User")
                .balance(new BigDecimal("200.00"))
                .accountType("CHECKING")
                .status(AccountStatus.ACTIVE.name())
                .build());

        TransferRequest transferRequest = TransferRequest.builder()
                .sourceAccountId(source.getId())
                .destinationAccountId(destination.getId())
                .amount(new BigDecimal("100.00"))
                .description("Integration test transfer")
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        mockMvc.perform(post("/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/{accountId}/transactions", source.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accountId").value(source.getId()))
                .andExpect(jsonPath("$[0].transactionType").value("DEBIT"))
                .andExpect(jsonPath("$[0].amount").value(100.00));
    }

        private static Map<String, String> loadDotEnvIfPresent() {
                Map<String, String> values = new HashMap<>();
                Path envPath = Path.of("backend", ".env");
                if (!Files.exists(envPath)) {
                        envPath = Path.of(".env");
                }
                if (!Files.exists(envPath)) {
                        return values;
                }

                try {
                        List<String> lines = Files.readAllLines(envPath);
                        for (String line : lines) {
                                String trimmed = line.trim();
                                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                                        continue;
                                }
                                int idx = trimmed.indexOf('=');
                                String key = trimmed.substring(0, idx).trim();
                                String value = trimmed.substring(idx + 1).trim();
                                values.put(key, value);
                        }
                } catch (Exception ignored) {
                        return values;
                }

                return values;
        }

        private static String firstNonBlank(String... candidates) {
                for (String candidate : candidates) {
                        if (candidate != null && !candidate.isBlank()) {
                                return candidate;
                        }
                }
                return null;
        }
}
