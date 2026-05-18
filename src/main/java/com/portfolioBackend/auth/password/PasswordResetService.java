package com.portfolioBackend.auth.password;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolioBackend.auth.user.User;
import com.portfolioBackend.auth.user.UserService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Gestiona la emision y consumo de tokens de recuperacion de contrasena.
 */
@Service
@Transactional
public class PasswordResetService {

    private final PasswordResetTokenRepository repo;
    private final UserService userService;
    private final int expirationMinutes;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(PasswordResetTokenRepository repo,
                                UserService userService,
                                @Value("${app.password-reset.expiration-minutes:30}") int expirationMinutes) {
        this.repo = repo;
        this.userService = userService;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Solicita un reset sin revelar si el email pertenece a una cuenta.
     */
    public void requestReset(String email) {
        userService.findByEmailIgnoreCase(email).ifPresent(user -> {
            // invalidar solicitudes previas del usuario (simple: borrar)
            repo.deleteAllByUserId(user.getId());

            String plain = generateOpaqueToken();
            String hash = sha256Url(plain);

            var prt = new PasswordResetToken();
            prt.setUser(user);
            prt.setTokenHash(hash);
            prt.setExpiresAt(Instant.now().plus(Duration.ofMinutes(expirationMinutes)));
            repo.save(prt);

            // Aqui se enviaria el email con el token plano.
            // e.g., mailService.sendPasswordReset(user.getEmail(), plain);
        });
    }

    /**
     * Aplica el cambio de contrasena usando un token de un solo uso.
     */
    public void reset(String tokenPlain, String newPassword) {
        String hash = sha256Url(tokenPlain);
        var prt = repo.findByTokenHashFetchUser(hash)
                .orElseThrow(() -> new RuntimeException("RESET_TOKEN_INVALID"));

        if (prt.isUsed() || prt.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("RESET_TOKEN_EXPIRED");
        }

        User u = prt.getUser();
        userService.forceChangePassword(u.getId(), newPassword);

        // Marcar token como usado
        prt.setUsed(true);
        repo.save(prt);
    }

    // --- utilidades privadas ---

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48]; // 384 bits
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Url(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("CANNOT_COMPUTE_SHA256", e);
        }
    }
}
