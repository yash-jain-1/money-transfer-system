package com.moneytransfer.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.security.user")
public class SecurityUserProperties {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}