package com.portfolioBackend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void corsConfigurationAllowsPortfolioAndLocalOrigins() {
        var config = new SecurityConfig("", secret());

        CorsConfiguration cors = config.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/auth/login"));

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).contains(
                "http://localhost:5173",
                "https://oscarpelegrina.com"
        );
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "PUT", "OPTIONS");
        assertThat(cors.getAllowCredentials()).isTrue();
    }

    @Test
    void beansUseConfiguredJwtSecretAndBcrypt() {
        var config = new SecurityConfig("", secret());

        assertThat(config.jwtDecoder()).isNotNull();
        assertThat(config.passwordEncoder().matches("secret", config.passwordEncoder().encode("secret"))).isTrue();
    }

    private static String secret() {
        return Base64.getEncoder()
                .encodeToString("test-secret-key-for-security-config".getBytes(StandardCharsets.UTF_8));
    }
}
