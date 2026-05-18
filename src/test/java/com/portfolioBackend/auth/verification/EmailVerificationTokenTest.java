package com.portfolioBackend.auth.verification;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationTokenTest {

    @Test
    void usedAndExpiredReflectTokenState() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setExpiresAt(Instant.now().plusSeconds(60));

        assertThat(token.isUsed()).isFalse();
        assertThat(token.isExpired()).isFalse();

        token.setUsedAt(Instant.now());
        token.setExpiresAt(Instant.now().minusSeconds(1));

        assertThat(token.isUsed()).isTrue();
        assertThat(token.isExpired()).isTrue();
    }
}
