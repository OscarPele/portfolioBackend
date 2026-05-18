package com.portfolioBackend.auth.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({UserService.class, UserServiceTest.PasswordConfig.class})
class UserServiceTest {

    private final UserService users;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Autowired
    UserServiceTest(UserService users, UserRepository userRepository, PasswordEncoder encoder) {
        this.users = users;
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Test
    void registerPersistsDisabledUserWithEncodedPassword() {
        User user = users.register("ana", "ana@example.com", "secret");

        assertThat(user.getId()).isNotNull();
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getPasswordHash()).isNotEqualTo("secret");
        assertThat(encoder.matches("secret", user.getPasswordHash())).isTrue();
    }

    @Test
    void registerRejectsDuplicateUsernameAndEmail() {
        users.register("ana", "ana@example.com", "secret");

        assertThatThrownBy(() -> users.register("ana", "other@example.com", "secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("USERNAME_EXISTS");
        assertThatThrownBy(() -> users.register("other", "ana@example.com", "secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("EMAIL_EXISTS");
    }

    @Test
    void authenticateAcceptsUsernameOrEmailOnlyWhenEnabled() {
        User user = users.register("ana", "ana@example.com", "secret");

        assertThatThrownBy(() -> users.authenticate("ana", "secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("EMAIL_NOT_VERIFIED");

        user.setEnabled(true);
        userRepository.saveAndFlush(user);

        assertThat(users.authenticate("ana", "secret").getId()).isEqualTo(user.getId());
        assertThat(users.authenticate("ana@example.com", "secret").getId()).isEqualTo(user.getId());
        assertThatThrownBy(() -> users.authenticate("ana", "bad"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("INVALID_CREDENTIALS");
    }

    @Test
    void changePasswordRequiresCurrentPassword() {
        User user = users.register("ana", "ana@example.com", "old-secret");

        assertThatThrownBy(() -> users.changePassword(user.getId(), "wrong", "new-secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("CURRENT_PASSWORD_INCORRECT");

        users.changePassword(user.getId(), "old-secret", "new-secret");

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(encoder.matches("new-secret", reloaded.getPasswordHash())).isTrue();
    }

    @Test
    void requireByIdAndForceChangePasswordUseStoredUser() {
        User user = users.register("ana", "ana@example.com", "old-secret");

        assertThat(users.requireById(user.getId()).getEmail()).isEqualTo("ana@example.com");
        assertThatThrownBy(() -> users.requireById(-1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("USER_NOT_FOUND");

        users.forceChangePassword(user.getId(), "forced-secret");

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(encoder.matches("forced-secret", reloaded.getPasswordHash())).isTrue();
    }

    @TestConfiguration
    static class PasswordConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
