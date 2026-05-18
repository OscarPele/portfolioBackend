package com.portfolioBackend.auth.password;

import com.portfolioBackend.auth.user.User;
import com.portfolioBackend.auth.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PasswordResetCleanup.class)
class PasswordResetCleanupTest {

    private final PasswordResetCleanup cleanup;
    private final PasswordResetTokenRepository tokens;
    private final UserRepository users;

    @Autowired
    PasswordResetCleanupTest(PasswordResetCleanup cleanup,
                             PasswordResetTokenRepository tokens,
                             UserRepository users) {
        this.cleanup = cleanup;
        this.tokens = tokens;
        this.users = users;
    }

    @Test
    void cleanDeletesExpiredAndUsedTokens() {
        User user = saveUser();
        saveToken(user, "expired", Instant.now().minusSeconds(1), false);
        saveToken(user, "used", Instant.now().plusSeconds(60), true);
        saveToken(user, "active", Instant.now().plusSeconds(60), false);

        cleanup.clean();

        assertThat(tokens.findAll())
                .singleElement()
                .satisfies(token -> assertThat(token.getTokenHash()).isEqualTo("active"));
    }

    private User saveUser() {
        User user = new User();
        user.setUsername("ana");
        user.setEmail("ana@example.com");
        user.setPasswordHash("hash");
        return users.saveAndFlush(user);
    }

    private void saveToken(User user, String hash, Instant expiresAt, boolean used) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash);
        token.setExpiresAt(expiresAt);
        token.setUsed(used);
        tokens.saveAndFlush(token);
    }
}
