package com.moneytransfer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for enabling repository scanning and audit support.
 * Enables Spring Data JPA repositories and entity auditing.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.moneytransfer.repository")
public class JpaConfig {
    // JPA configuration is primarily handled by application.yml
    // This class serves as an anchor point for future JPA customizations
}
