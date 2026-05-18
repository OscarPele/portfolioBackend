package com.portfolioBackend.notifications;

import com.portfolioBackend.auth.mail.MailSenderPort;
import com.portfolioBackend.auth.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerAlertServiceTest {

    @Test
    void notifyContactFormEscapesHtmlAndSendsToOwner() throws InterruptedException {
        var mail = new CapturingMailSender();
        var service = new OwnerAlertService(mail, "owner@example.com");

        service.notifyContactForm("<Ana>", "ana@example.com", "<script>alert(1)</script>");

        assertThat(mail.await()).isTrue();
        assertThat(mail.to).isEqualTo("owner@example.com");
        assertThat(mail.subject).isEqualTo("Nuevo mensaje de contacto: <Ana>");
        assertThat(mail.htmlBody).contains("&lt;Ana&gt;");
        assertThat(mail.htmlBody).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void notifyNewRegistrationIncludesUserData() throws InterruptedException {
        var mail = new CapturingMailSender();
        var service = new OwnerAlertService(mail, "owner@example.com");
        User user = new User();
        user.setUsername("ana");
        user.setEmail("ana@example.com");
        user.setCreatedAt(Instant.parse("2026-05-18T10:00:00Z"));

        service.notifyNewRegistration(user);

        assertThat(mail.await()).isTrue();
        assertThat(mail.subject).isEqualTo("Nuevo registro en el ERP: @ana");
        assertThat(mail.htmlBody).contains("ana@example.com");
        assertThat(mail.htmlBody).contains("2026-05-18T10:00:00Z");
    }

    private static class CapturingMailSender implements MailSenderPort {
        private final CountDownLatch sent = new CountDownLatch(1);
        volatile String to;
        volatile String subject;
        volatile String htmlBody;

        @Override
        public void send(String to, String subject, String htmlBody) {
            this.to = to;
            this.subject = subject;
            this.htmlBody = htmlBody;
            sent.countDown();
        }

        boolean await() throws InterruptedException {
            return sent.await(2, TimeUnit.SECONDS);
        }
    }
}
