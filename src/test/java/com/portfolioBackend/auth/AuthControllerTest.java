package com.portfolioBackend.auth;

import com.portfolioBackend.auth.password.PasswordResetService;
import com.portfolioBackend.auth.user.User;
import com.portfolioBackend.auth.user.UserService;
import com.portfolioBackend.auth.verification.EmailVerificationService;
import com.portfolioBackend.notifications.OwnerAlertService;
import com.portfolioBackend.security.JWTService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthControllerTest {

    @Test
    void registerReturnsUserAndTriggersNotifications() {
        var users = new FakeUserService();
        var verification = new FakeEmailVerificationService();
        var alerts = new FakeOwnerAlertService();
        var controller = new AuthController(users, new FakePasswordResetService(), verification, alerts, new FakeJwtService());

        var response = controller.register(Map.of(
                "username", "ana",
                "email", "ana@example.com",
                "password", "secret"
        ));
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.get("username")).isEqualTo("ana");
        assertThat(users.registeredPassword).isEqualTo("secret");
        assertThat(verification.sentUser).isSameAs(users.user);
        assertThat(alerts.registeredUser).isSameAs(users.user);
    }

    @Test
    void loginReturnsBearerToken() {
        var users = new FakeUserService();
        var controller = new AuthController(users, new FakePasswordResetService(),
                new FakeEmailVerificationService(), new FakeOwnerAlertService(), new FakeJwtService());

        var response = controller.login(Map.of("usernameOrEmail", "ana", "password", "secret"));
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.get("tokenType")).isEqualTo("Bearer");
        assertThat(body.get("accessToken")).isEqualTo("signed-token");
        assertThat(users.authenticatedPassword).isEqualTo("secret");
    }

    @Test
    void passwordEndpointsDelegateAndReturnNoContent() {
        var resets = new FakePasswordResetService();
        var controller = new AuthController(new FakeUserService(), resets,
                new FakeEmailVerificationService(), new FakeOwnerAlertService(), new FakeJwtService());

        assertThat(controller.forgotPassword(Map.of("email", "ana@example.com")).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.resetPassword(Map.of("token", "token", "newPassword", "new-secret")).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(resets.requestedEmail).isEqualTo("ana@example.com");
        assertThat(resets.resetToken).isEqualTo("token");
        assertThat(resets.resetPassword).isEqualTo("new-secret");
    }

    @Test
    void changeOwnPasswordRequiresUidClaim() {
        var users = new FakeUserService();
        var controller = new AuthController(users, new FakePasswordResetService(),
                new FakeEmailVerificationService(), new FakeOwnerAlertService(), new FakeJwtService());

        var response = controller.changeOwnPassword(jwtWithUid(5L), Map.of(
                "currentPassword", "old-secret",
                "newPassword", "new-secret"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(users.changedUserId).isEqualTo(5L);
        assertThat(users.changedCurrentPassword).isEqualTo("old-secret");
        assertThat(users.changedNewPassword).isEqualTo("new-secret");
        assertThatThrownBy(() -> controller.changeOwnPassword(jwtWithoutUid(), Map.of()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("JWT sin uid");
    }

    private Jwt jwtWithUid(Long uid) {
        return jwtBuilder().claim("uid", uid).build();
    }

    private Jwt jwtWithoutUid() {
        return jwtBuilder().build();
    }

    private Jwt.Builder jwtBuilder() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60));
    }

    private static User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("ana");
        user.setEmail("ana@example.com");
        user.setCreatedAt(Instant.parse("2026-05-18T10:00:00Z"));
        return user;
    }

    private static class FakeUserService extends UserService {
        final User user = buildUser();
        String registeredPassword;
        String authenticatedPassword;
        Long changedUserId;
        String changedCurrentPassword;
        String changedNewPassword;

        FakeUserService() {
            super(null, null);
        }

        @Override
        public User register(String username, String email, String rawPassword) {
            registeredPassword = rawPassword;
            return user;
        }

        @Override
        public User authenticate(String usernameOrEmail, String rawPassword) {
            authenticatedPassword = rawPassword;
            return user;
        }

        @Override
        public void changePassword(Long id, String currentPassword, String newPassword) {
            changedUserId = id;
            changedCurrentPassword = currentPassword;
            changedNewPassword = newPassword;
        }
    }

    private static class FakePasswordResetService extends PasswordResetService {
        String requestedEmail;
        String resetToken;
        String resetPassword;

        FakePasswordResetService() {
            super(null, null, 30);
        }

        @Override
        public void requestReset(String email) {
            requestedEmail = email;
        }

        @Override
        public void reset(String tokenPlain, String newPassword) {
            resetToken = tokenPlain;
            resetPassword = newPassword;
        }
    }

    private static class FakeEmailVerificationService extends EmailVerificationService {
        User sentUser;

        FakeEmailVerificationService() {
            super(null, null, null, 24, "", "", "");
        }

        @Override
        public void send(User u) {
            sentUser = u;
        }
    }

    private static class FakeOwnerAlertService extends OwnerAlertService {
        User registeredUser;

        FakeOwnerAlertService() {
            super((to, subject, htmlBody) -> { }, "owner@example.com");
        }

        @Override
        public void notifyNewRegistration(User user) {
            registeredUser = user;
        }
    }

    private static class FakeJwtService extends JWTService {
        FakeJwtService() {
            super(Base64.getEncoder().encodeToString(
                    "test-secret-key-for-auth-controller".getBytes(StandardCharsets.UTF_8)), 3600, "test");
        }

        @Override
        public String generate(String username, Map<String, Object> extraClaims) {
            return "signed-token";
        }
    }
}
