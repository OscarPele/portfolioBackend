package com.portfolioBackend.auth.verification;

import com.portfolioBackend.auth.user.User;
import com.portfolioBackend.auth.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationControllerTest {

    @Test
    void requestSendsVerificationOnlyForPendingUsers() {
        var verification = new FakeEmailVerificationService();
        var users = new FakeUserService();
        var controller = new EmailVerificationController(verification, users);

        var response = controller.request(new EmailVerificationController.VerifyEmailRequest("ana@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(verification.sentUser).isSameAs(users.user);

        verification.sentUser = null;
        users.user.setEnabled(true);
        controller.request(new EmailVerificationController.VerifyEmailRequest("ana@example.com"));
        assertThat(verification.sentUser).isNull();
    }

    @Test
    void requestDoesNotRevealMissingUsers() {
        var verification = new FakeEmailVerificationService();
        var users = new FakeUserService();
        users.user = null;
        var controller = new EmailVerificationController(verification, users);

        var response = controller.request(new EmailVerificationController.VerifyEmailRequest("missing@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(verification.sentUser).isNull();
    }

    @Test
    void verifyRedirectsToServiceUrl() {
        var verification = new FakeEmailVerificationService();
        var controller = new EmailVerificationController(verification, new FakeUserService());

        var response = controller.verify("plain-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation().toString()).isEqualTo("http://front.test/verified");
        assertThat(verification.confirmedToken).isEqualTo("plain-token");
    }

    private static class FakeUserService extends UserService {
        User user = pendingUser();

        FakeUserService() {
            super(null, null);
        }

        @Override
        public Optional<User> findByEmailIgnoreCase(String email) {
            return Optional.ofNullable(user);
        }

        private static User pendingUser() {
            User user = new User();
            user.setEmail("ana@example.com");
            user.setUsername("ana");
            return user;
        }
    }

    private static class FakeEmailVerificationService extends EmailVerificationService {
        User sentUser;
        String confirmedToken;

        FakeEmailVerificationService() {
            super(null, null, null, 24, "", "http://front.test/verified", "http://front.test/error");
        }

        @Override
        public void send(User u) {
            sentUser = u;
        }

        @Override
        public String confirmAndGetRedirectUrl(String plainToken) {
            confirmedToken = plainToken;
            return "http://front.test/verified";
        }
    }
}
