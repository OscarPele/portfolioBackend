package com.portfolioBackend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    @Test
    void returnsNullWhenJwtOrUidIsMissing() {
        assertThat(JwtUtils.getUid(null)).isNull();
        assertThat(JwtUtils.getUid(jwtWithoutUid())).isNull();
    }

    @Test
    void readsNumericAndStringUidClaims() {
        assertThat(JwtUtils.getUid(jwtWithUid(42))).isEqualTo(42L);
        assertThat(JwtUtils.getUid(jwtWithUid("7"))).isEqualTo(7L);
    }

    @Test
    void returnsNullWhenUidCannotBeParsed() {
        assertThat(JwtUtils.getUid(jwtWithUid("abc"))).isNull();
    }

    private Jwt jwtWithoutUid() {
        return baseJwt().build();
    }

    private Jwt jwtWithUid(Object uid) {
        return baseJwt().claim("uid", uid).build();
    }

    private Jwt.Builder baseJwt() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60));
    }
}
