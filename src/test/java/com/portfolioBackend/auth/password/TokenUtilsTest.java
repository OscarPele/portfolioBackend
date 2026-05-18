package com.portfolioBackend.auth.password;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenUtilsTest {

    @Test
    void newBase64UrlTokenIsUrlSafeAndRandomEnoughForConsecutiveCalls() {
        String first = TokenUtils.newBase64UrlToken();
        String second = TokenUtils.newBase64UrlToken();

        assertThat(first).matches("[A-Za-z0-9_-]+");
        assertThat(second).matches("[A-Za-z0-9_-]+");
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void sha256Base64UrlIsStable() {
        assertThat(TokenUtils.sha256Base64Url("token"))
                .isEqualTo(TokenUtils.sha256Base64Url("token"));
        assertThat(TokenUtils.sha256Base64Url("token"))
                .isNotEqualTo(TokenUtils.sha256Base64Url("other"));
    }
}
