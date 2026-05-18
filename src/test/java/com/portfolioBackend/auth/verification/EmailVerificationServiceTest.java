package com.portfolioBackend.auth.verification;

import com.portfolioBackend.auth.mail.MailSenderPort;
import com.portfolioBackend.auth.user.User;
import com.portfolioBackend.auth.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({EmailVerificationService.class, EmailVerificationServiceTest.MailConfig.class})
@TestPropertySource(properties = {
        "app.verify-email.ttl-hours=24",
        "app.verify-email.backend-verify-url=http://backend.test/auth/verify-email",
        "app.verify-email.frontend-success-url=http://front.test/verified",
        "app.verify-email.frontend-error-url=http://front.test/verify-error"
})
class EmailVerificationServiceTest {

    private final EmailVerificationService verification;
    private final EmailVerificationTokenRepository tokens;
    private final UserRepository users;
    private final CapturingMailSender mail;

    @Autowired
    EmailVerificationServiceTest(EmailVerificationService verification,
                                 EmailVerificationTokenRepository tokens,
                                 UserRepository users,
                                 CapturingMailSender mail) {
        this.verification = verification;
        this.tokens = tokens;
        this.users = users;
        this.mail = mail;
    }

    @Test
    void sendCreatesOnePendingTokenAndEmailLink() {
        User user = saveUser("ana", "ana@example.com");

        verification.send(user);
        String firstHash = tokens.findAll().getFirst().getTokenHash();
        verification.send(user);

        assertThat(tokens.findAll()).hasSize(1);
        assertThat(tokens.findAll().getFirst().getTokenHash()).isNotEqualTo(firstHash);
        assertThat(mail.to).isEqualTo("ana@example.com");
        assertThat(mail.subject).isEqualTo("Verifica tu correo");
        assertThat(mail.htmlBody).contains("http://backend.test/auth/verify-email?token=");
    }

    @Test
    void confirmConsumesTokenAndEnablesUser() {
        User user = saveUser("ana", "ana@example.com");
        verification.send(user);
        String plainToken = extractToken(mail.htmlBody);

        String redirect = verification.confirmAndGetRedirectUrl(plainToken);

        assertThat(redirect).isEqualTo("http://front.test/verified");
        assertThat(users.findById(user.getId()).orElseThrow().isEnabled()).isTrue();
        assertThat(tokens.findAll()).singleElement().satisfies(token -> assertThat(token.isUsed()).isTrue());
    }

    @Test
    void confirmReturnsErrorRedirectsForInvalidAndExpiredTokens() {
        assertThat(verification.confirmAndGetRedirectUrl("missing"))
                .isEqualTo("http://front.test/verify-error?reason=INVALID_TOKEN");

        User user = saveUser("ana", "ana@example.com");
        EmailVerificationToken expired = new EmailVerificationToken();
        expired.setUser(user);
        expired.setTokenHash(sha256("expired-token"));
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        tokens.saveAndFlush(expired);

        assertThat(verification.confirmAndGetRedirectUrl("expired-token"))
                .isEqualTo("http://front.test/verify-error?reason=TOKEN_EXPIRED");
        assertThat(tokens.findById(expired.getId())).isEmpty();
    }

    @Test
    void confirmApiThrowsDomainCodes() {
        assertThatThrownBy(() -> verification.confirm("missing"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("INVALID_TOKEN");
    }

    private User saveUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hash");
        return users.saveAndFlush(user);
    }

    private static String extractToken(String html) {
        var matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(html);
        assertThat(matcher.find()).as("verification token in email").isTrue();
        return matcher.group(1);
    }

    private static String sha256(String input) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @TestConfiguration
    static class MailConfig {
        @Bean
        CapturingMailSender mailSender() {
            return new CapturingMailSender();
        }
    }

    static class CapturingMailSender implements MailSenderPort {
        String to;
        String subject;
        String htmlBody;

        @Override
        public void send(String to, String subject, String htmlBody) {
            this.to = to;
            this.subject = subject;
            this.htmlBody = htmlBody;
        }
    }
}
