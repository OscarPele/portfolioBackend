package com.portfolioBackend.auth.password;

import com.portfolioBackend.auth.user.User;
import com.portfolioBackend.auth.user.UserRepository;
import com.portfolioBackend.auth.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({PasswordResetService.class, UserService.class, PasswordResetServiceTest.PasswordConfig.class})
@TestPropertySource(properties = "app.password-reset.expiration-minutes=15")
class PasswordResetServiceTest {

    private final PasswordResetService resets;
    private final PasswordResetTokenRepository tokens;
    private final UserService users;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Autowired
    PasswordResetServiceTest(PasswordResetService resets,
                             PasswordResetTokenRepository tokens,
                             UserService users,
                             UserRepository userRepository,
                             PasswordEncoder encoder) {
        this.resets = resets;
        this.tokens = tokens;
        this.users = users;
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Test
    void requestResetCreatesOnlyOneActiveTokenPerUser() {
        User user = users.register("ana", "ana@example.com", "secret");

        resets.requestReset("ANA@example.com");
        resets.requestReset("ana@example.com");

        assertThat(tokens.findAll()).singleElement().satisfies(token -> {
            assertThat(token.getUser().getId()).isEqualTo(user.getId());
            assertThat(token.isUsed()).isFalse();
            assertThat(token.getExpiresAt()).isAfter(Instant.now());
        });
    }

    @Test
    void requestResetIgnoresUnknownEmails() {
        resets.requestReset("missing@example.com");

        assertThat(tokens.findAll()).isEmpty();
    }

    @Test
    void resetConsumesValidTokenAndChangesPassword() {
        User user = users.register("ana", "ana@example.com", "old-secret");
        user.setEnabled(true);
        userRepository.saveAndFlush(user);
        String plainToken = "plain-reset-token";
        PasswordResetToken token = saveResetToken(user, plainToken, Instant.now().plusSeconds(60));

        resets.reset(plainToken, "new-secret");

        assertThat(tokens.findById(token.getId()).orElseThrow().isUsed()).isTrue();
        assertThat(users.authenticate("ana", "new-secret").getId()).isEqualTo(user.getId());
        assertThatThrownBy(() -> users.authenticate("ana", "old-secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("INVALID_CREDENTIALS");
    }

    @Test
    void resetRejectsExpiredToken() {
        User user = users.register("ana", "ana@example.com", "old-secret");
        saveResetToken(user, "expired-token", Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> resets.reset("expired-token", "new-secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("RESET_TOKEN_EXPIRED");
        assertThat(encoder.matches("old-secret", users.requireById(user.getId()).getPasswordHash())).isTrue();
    }

    @Test
    void resetRejectsUnknownToken() {
        assertThatThrownBy(() -> resets.reset("missing-token", "new-secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("RESET_TOKEN_INVALID");
    }

    private PasswordResetToken saveResetToken(User user, String plainToken, Instant expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(sha256Url(plainToken));
        token.setExpiresAt(expiresAt);
        return tokens.saveAndFlush(token);
    }

    private static String sha256Url(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @TestConfiguration
    static class PasswordConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
