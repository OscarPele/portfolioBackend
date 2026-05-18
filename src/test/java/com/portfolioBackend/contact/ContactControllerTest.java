package com.portfolioBackend.contact;

import com.portfolioBackend.notifications.OwnerAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ContactControllerTest {

    @Test
    void rejectsBlankFields() {
        var alerts = new CapturingOwnerAlertService();
        var controller = new ContactController(alerts);

        var response = controller.contact(new ContactRequest(" ", "demo@example.com", "Hola"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(alerts.calls).isZero();
    }

    @Test
    void trimsAndSendsValidMessages() {
        var alerts = new CapturingOwnerAlertService();
        var controller = new ContactController(alerts);

        var response = controller.contact(new ContactRequest("  Ana  ", " ana@example.com ", "  Hola  "));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(alerts.calls).isEqualTo(1);
        assertThat(alerts.name).isEqualTo("Ana");
        assertThat(alerts.email).isEqualTo("ana@example.com");
        assertThat(alerts.message).isEqualTo("Hola");
    }

    private static class CapturingOwnerAlertService extends OwnerAlertService {
        int calls;
        String name;
        String email;
        String message;

        CapturingOwnerAlertService() {
            super((to, subject, htmlBody) -> { }, "owner@example.com");
        }

        @Override
        public void notifyContactForm(String name, String email, String message) {
            this.calls++;
            this.name = name;
            this.email = email;
            this.message = message;
        }
    }
}
