package com.moneytransfer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneytransfer.config.SecurityUserProperties;
import com.moneytransfer.domain.entity.Account;
import com.moneytransfer.domain.entity.User;
import com.moneytransfer.domain.entity.UserRole;
import com.moneytransfer.domain.status.AccountStatus;
import com.moneytransfer.dto.request.LoginRequest;
import com.moneytransfer.dto.request.TransferRequest;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.TransactionLogRepository;
import com.moneytransfer.repository.UserRepository;
import com.moneytransfer.util.JwtUtil;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Rate Limiting Integration Tests")
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SecurityUserProperties securityUserProperties;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // @Autowired
    // // private Map<String, Bucket> rateLimitBuckets;

    @Autowired
    private java.util.Map<String, io.github.bucket4j.Bucket> rateLimitBuckets;

    private Long account1Id;
    private Long account2Id;
    private Long account3Id;

    private User testUser;

    private static final String AUTH_ENDPOINT = "/auth/login";
    private static final String TRANSFER_ENDPOINT = "/transfers";

    @BeforeEach
    void setUp() {
        // Clear rate limit buckets from previous tests
        rateLimitBuckets.clear();

        // Clean up from previous tests
        transactionLogRepository.deleteAll();
        accountRepository.deleteAll();

        String username = securityUserProperties.getUsername();
        String password = securityUserProperties.getPassword();
        testUser = userRepository.findByUsername(username)
            .orElseGet(() -> userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(username + "@example.com")
                .fullName("Rate Limit Test User")
                .role(UserRole.USER)
                .enabled(true)
                .build()));

        // Use timestamp for unique account numbers
        long timestamp = System.currentTimeMillis();

        // Create test accounts
        Account account1 = Account.builder()
                .accountNumber("ACC" + timestamp + "001")
                .accountHolder("Test User 1")
                .balance(new BigDecimal("5000.00"))
                .accountType("CHECKING")
                .status(AccountStatus.ACTIVE.name())
            .owner(testUser)
                .build();
        Account savedAccount1 = accountRepository.save(account1);
        account1Id = savedAccount1.getId();

        Account account2 = Account.builder()
                .accountNumber("ACC" + timestamp + "002")
                .accountHolder("Test User 2")
                .balance(new BigDecimal("5000.00"))
                .accountType("CHECKING")
                .status(AccountStatus.ACTIVE.name())
            .owner(testUser)
                .build();
        Account savedAccount2 = accountRepository.save(account2);
        account2Id = savedAccount2.getId();

        Account account3 = Account.builder()
                .accountNumber("ACC" + timestamp + "003")
                .accountHolder("Test User 3")
                .balance(new BigDecimal("5000.00"))
                .accountType("CHECKING")
                .status(AccountStatus.ACTIVE.name())
            .owner(testUser)
                .build();
        Account savedAccount3 = accountRepository.save(account3);
        account3Id = savedAccount3.getId();
    }

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

    // Helper method to get a valid token
    private String getValidToken() {
        // Generate a token directly using JwtUtil for testing
        return jwtUtil.generateToken(securityUserProperties.getUsername(), "USER");
    }

    // ==================== AUTH ENDPOINT RATE LIMITING ====================

    @Test
    @DisplayName("Auth endpoint: Exceed rate limit (5 attempts/minute) → returns 429")
    void testAuthRateLimitExceeded() throws Exception {
        String username = "attacker-" + System.currentTimeMillis();

        // Make 5 login attempts (should fail with 401, not 429)
        for (int i = 0; i < 5; i++) {
            LoginRequest request = new LoginRequest();
            request.setUsername(username);
            request.setPassword("wrong");

            mockMvc.perform(post(AUTH_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();
        }

        // 6th attempt should be rate limited (429)
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword("wrong");

        mockMvc.perform(post(AUTH_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andReturn();
    }

    @Test
    @DisplayName("Auth endpoint: Under rate limit → returns 401 (not 429)")
    void testAuthUnderRateLimit() throws Exception {
        String username = "user-" + System.currentTimeMillis();

        // Make 3 login attempts (under 5 limit) - should get 401, not 429
        for (int i = 0; i < 3; i++) {
            LoginRequest request = new LoginRequest();
            request.setUsername(username);
            request.setPassword("wrong");

            mockMvc.perform(post(AUTH_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized()) // 401, not rate limited
                    .andReturn();
        }
    }

    @Test
    @DisplayName("Auth endpoint: Separate users → separate rate limits")
    void testAuthSeparateUserLimits() throws Exception {
        String user1 = "user1-" + System.currentTimeMillis();
        String user2 = "user2-" + System.currentTimeMillis();

        // User1: consume 5 attempts
        for (int i = 0; i < 5; i++) {
            LoginRequest request = new LoginRequest();
            request.setUsername(user1);
            request.setPassword("wrong");
            mockMvc.perform(post(AUTH_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();
        }

        // User1: 6th attempt is rate limited
        LoginRequest limitedRequest = new LoginRequest();
        limitedRequest.setUsername(user1);
        limitedRequest.setPassword("wrong");
        mockMvc.perform(post(AUTH_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(limitedRequest)))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        // User2: should still be able to make requests
        LoginRequest user2Request = new LoginRequest();
        user2Request.setUsername(user2);
        user2Request.setPassword("wrong");
        mockMvc.perform(post(AUTH_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2Request)))
                .andExpect(status().isUnauthorized()) // 401, not rate limited
                .andReturn();
    }

    // ==================== TRANSFER ENDPOINT RATE LIMITING ====================

    @Test
    @DisplayName("Transfer endpoint: Exceed rate limit (10 transfers/minute) → 429")
    void testTransferRateLimitExceeded() throws Exception {
        String token = getValidToken();

        // Make 10 transfer attempts
        for (int i = 0; i < 10; i++) {
            TransferRequest request = new TransferRequest();
            request.setSourceAccountId(account1Id);
            request.setDestinationAccountId(account2Id);
            request.setAmount(BigDecimal.valueOf(10));
            request.setIdempotencyKey(UUID.randomUUID().toString());

            mockMvc.perform(post(TRANSFER_ENDPOINT)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();
        }

        // 11th attempt should be rate limited (429)
        TransferRequest request = new TransferRequest();
        request.setSourceAccountId(account1Id);
        request.setDestinationAccountId(account2Id);
        request.setAmount(BigDecimal.valueOf(10));
        request.setIdempotencyKey(UUID.randomUUID().toString());

        mockMvc.perform(post(TRANSFER_ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.description", containsString("rate limit")))
                .andReturn();
    }

    @Test
    @DisplayName("Transfer endpoint: Under rate limit → 200/201")
    void testTransferUnderRateLimit() throws Exception {
        String token = getValidToken();

        // Make 5 transfer attempts (under 10 limit)
        for (int i = 0; i < 5; i++) {
            TransferRequest request = new TransferRequest();
            request.setSourceAccountId(account1Id);
            request.setDestinationAccountId(account2Id);
            request.setAmount(BigDecimal.valueOf(10));
            request.setIdempotencyKey(UUID.randomUUID().toString());

            mockMvc.perform(post(TRANSFER_ENDPOINT)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();
        }
    }

    // ==================== ACCOUNT READ ENDPOINT RATE LIMITING ====================

    @Test
    @DisplayName("Account read: Exceed rate limit (60 reads/minute) → 429")
    void testAccountReadRateLimitExceeded() throws Exception {
        String token = getValidToken();

        // Make 60 read attempts
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get("/accounts/" + account1Id)
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
        }

        // 61st attempt should be rate limited (429)
        mockMvc.perform(get("/accounts/" + account1Id)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andReturn();
    }

    @Test
    @DisplayName("Account balance: Under rate limit → 200")
    void testAccountBalanceUnderRateLimit() throws Exception {
        String token = getValidToken();

        // Make 30 balance read attempts (under 60 limit)
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/accounts/" + account1Id + "/balance")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
        }
    }

    @Test
    @DisplayName("Account transactions: Under rate limit → 200")
    void testAccountTransactionsUnderRateLimit() throws Exception {
        String token = getValidToken();

        // Make 30 transaction read attempts (under 60 limit)
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/accounts/" + account1Id + "/transactions")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
        }
    }

    // ==================== AUTHENTICATION & RATE LIMITING ====================

    @Test
    @DisplayName("Unauthenticated request: returns 401 (before rate limiting)")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/accounts/" + account1Id))
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("Invalid token: returns 401 (before rate limiting)")
    void testInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/accounts/" + account1Id)
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    // ==================== PUBLIC ENDPOINTS ====================

    @Test
    @DisplayName("Swagger UI: Public endpoint, no authentication required")
    void testSwaggerUiPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DisplayName("OpenAPI spec: Public endpoint, no authentication required")
    void testOpenApiSpecPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
    }

    // ==================== HELPER METHODS ====================

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static Map<String, String> loadDotEnvIfPresent() {
        Map<String, String> env = new HashMap<>();
        try {
            Path dotEnvPath = Path.of(".env");
            if (Files.exists(dotEnvPath)) {
                Files.readAllLines(dotEnvPath).forEach(line -> {
                    if (!line.isBlank() && !line.startsWith("#")) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            env.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                });
            }
        } catch (Exception e) {
            // Ignore if .env doesn't exist
        }
        return env;
    }
}
