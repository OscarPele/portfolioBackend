package com.portfolioBackend.notifications;

import com.portfolioBackend.auth.mail.MailSenderPort;
import com.portfolioBackend.auth.user.User;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Envia avisos internos al propietario del portfolio.
 */
@Service
public class OwnerAlertService {

    private final MailSenderPort mailSender;
    private final String ownerEmail;

    public OwnerAlertService(
            MailSenderPort mailSender,
            @Value("${app.notifications.owner-email:oscarpear99@gmail.com}") String ownerEmail
    ) {
        this.mailSender = mailSender;
        this.ownerEmail = ownerEmail;
    }

    /**
     * Notifica que se ha registrado un usuario nuevo.
     */
    public void notifyNewRegistration(User user) {
        String subject = "Nuevo registro en el ERP: @" + safe(user.getUsername());
        String html = """
                <h2>Nuevo registro en el ERP</h2>
                <p>Se ha registrado un nuevo usuario en tu portfolio.</p>
                <ul>
                  <li><strong>Username:</strong> %s</li>
                  <li><strong>Email:</strong> %s</li>
                  <li><strong>Fecha:</strong> %s</li>
                </ul>
                <p>Puedes revisar su actividad entrando en tu portfolio.</p>
                """.formatted(
                escapeHtml(safe(user.getUsername())),
                escapeHtml(safe(user.getEmail())),
                escapeHtml(formatInstant(user.getCreatedAt()))
        );

        sendAsync(subject, html);
    }

    /**
     * Notifica un mensaje recibido desde el formulario de contacto.
     */
    public void notifyContactForm(String name, String email, String message) {
        String subject = "Nuevo mensaje de contacto: " + safe(name);
        String html = """
                <h2>Nuevo mensaje desde el portfolio</h2>
                <ul>
                  <li><strong>Nombre:</strong> %s</li>
                  <li><strong>Email:</strong> %s</li>
                </ul>
                <p><strong>Mensaje:</strong></p>
                <blockquote style="margin:0;padding:12px 16px;border-left:4px solid #d1d5db;background:#f8fafc;">%s</blockquote>
                """.formatted(
                escapeHtml(safe(name)),
                escapeHtml(safe(email)),
                escapeHtml(safe(message))
        );

        sendAsync(subject, html);
    }

    private void sendAsync(String subject, String html) {
        CompletableFuture.runAsync(() -> {
            try {
                mailSender.send(ownerEmail, subject, html);
            } catch (Exception exception) {
                System.err.println("[OWNER_ALERT_SERVICE] Error enviando aviso: " + exception.getMessage());
            }
        });
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String formatInstant(Instant instant) {
        return instant == null ? "-" : instant.toString();
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
