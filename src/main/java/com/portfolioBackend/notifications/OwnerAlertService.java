package com.portfolioBackend.notifications;

import com.portfolioBackend.auth.mail.MailSenderPort;
import com.portfolioBackend.auth.user.User;
import com.portfolioBackend.auth.user.UserService;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OwnerAlertService {

    private final MailSenderPort mailSender;
    private final UserService userService;
    private final String ownerEmail;

    public OwnerAlertService(
            MailSenderPort mailSender,
            UserService userService,
            @Value("${app.notifications.owner-email:oscarpear99@gmail.com}") String ownerEmail
    ) {
        this.mailSender = mailSender;
        this.userService = userService;
        this.ownerEmail = ownerEmail;
    }

    public void notifyNewRegistration(User user) {
        String subject = "Nuevo registro en Demo 1: @" + safe(user.getUsername());
        String html = """
                <h2>Nuevo registro en Demo 1</h2>
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

    public void notifyChatMessage(long userId, String messageText) {
        CompletableFuture.runAsync(() -> {
            try {
                User user = userService.requireById(userId);
                String subject = "Nuevo mensaje en Demo 2: @" + safe(user.getUsername());
                String html = """
                        <h2>Nuevo mensaje en el chat del portfolio</h2>
                        <p>Un usuario autenticado ha enviado un mensaje en Demo 2.</p>
                        <ul>
                          <li><strong>Username:</strong> %s</li>
                          <li><strong>Email:</strong> %s</li>
                          <li><strong>Fecha:</strong> %s</li>
                        </ul>
                        <p><strong>Mensaje:</strong></p>
                        <blockquote style="margin:0;padding:12px 16px;border-left:4px solid #d1d5db;background:#f8fafc;">%s</blockquote>
                        <p>Si quieres responderle directamente, entra en la demo 2 del portfolio.</p>
                        """.formatted(
                        escapeHtml(safe(user.getUsername())),
                        escapeHtml(safe(user.getEmail())),
                        escapeHtml(formatInstant(Instant.now())),
                        escapeHtml(safe(messageText))
                );

                mailSender.send(ownerEmail, subject, html);
            } catch (Exception exception) {
                System.err.println("[OWNER_ALERT_SERVICE] Error enviando aviso de chat: " + exception.getMessage());
            }
        });
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
