package com.portfolioBackend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    void mapsKnownDomainErrorsToHttpStatus() {
        assertStatus("EMAIL_EXISTS", HttpStatus.BAD_REQUEST);
        assertStatus("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
        assertStatus("USER_NOT_FOUND", HttpStatus.NOT_FOUND);
        assertStatus("FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    @Test
    void mapsUnknownErrorsToServerError() {
        var response = handler.handleAuthExceptions(new RuntimeException("UNEXPECTED"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("code", "UNEXPECTED");
    }

    private void assertStatus(String code, HttpStatus expected) {
        var response = handler.handleAuthExceptions(new RuntimeException(code));

        assertThat(response.getStatusCode()).isEqualTo(expected);
        assertThat(response.getBody()).containsEntry("code", code);
    }
}
