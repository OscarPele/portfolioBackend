package com.portfolioBackend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JWTServiceTest {

    private static final String RAW_SECRET = "test-secret-key-for-jwt-service-32-bytes";
    private static final String SECRET_B64 = Base64.getEncoder()
            .encodeToString(RAW_SECRET.getBytes(StandardCharsets.UTF_8));

    @Test
    void generatesDecodableJwtWithExpectedClaims() {
        var service = new JWTService(SECRET_B64, 120, "test-issuer");

        String token = service.generate("ana", Map.of("uid", 5L));
        var decoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(Base64.getDecoder().decode(SECRET_B64), "HmacSHA256"))
                .build();
        var decoded = decoder.decode(token);

        assertThat(decoded.getClaimAsString("iss")).isEqualTo("test-issuer");
        assertThat(decoded.getSubject()).isEqualTo("ana");
        assertThat(((Number) decoded.getClaim("uid")).longValue()).isEqualTo(5L);
        assertThat(Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt()).getSeconds()).isEqualTo(120);
        assertThat(service.getExpirationSeconds()).isEqualTo(120);
    }
}
