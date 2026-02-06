package com.moneytransfer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneytransfer.config.SecurityUserProperties;
import com.moneytransfer.domain.entity.User;
import com.moneytransfer.domain.entity.UserRole;
import com.moneytransfer.dto.request.LoginRequest;
import com.moneytransfer.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationIntegrationTest {

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
    private SecurityUserProperties securityUserProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        String username = securityUserProperties.getUsername();
        String password = securityUserProperties.getPassword();

        userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(User.builder()
                        .username(username)
                        .password(passwordEncoder.encode(password))
                        .email(username + "@example.com")
                        .fullName("Integration Test User")
                        .role(UserRole.USER)
                        .enabled(true)
                        .build()));
    }

    @Test
    @DisplayName("✅ POST /auth/login returns JWT")
    void loginReturnsJwt() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username(securityUserProperties.getUsername())
                .password(securityUserProperties.getPassword())
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("❌ GET /transfers/health without JWT returns 401")
    void healthWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/transfers/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("✅ GET /transfers/health with JWT returns 200")
    void healthWithJwtReturnsOk() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(get("/transfers/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("❌ GET /transfers/health with invalid JWT returns 401")
    void healthWithInvalidJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/transfers/health")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndGetToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username(securityUserProperties.getUsername())
                .password(securityUserProperties.getPassword())
                .build();

        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<?, ?> response = objectMapper.readValue(responseBody, Map.class);
        return response.get("token").toString();
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