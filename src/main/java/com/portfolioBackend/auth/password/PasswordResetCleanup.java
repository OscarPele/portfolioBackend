package com.portfolioBackend.auth.password;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Tarea programada que limpia tokens de recuperacion no reutilizables.
 */
@Service
public class PasswordResetCleanup {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetCleanup.class);
    private final PasswordResetTokenRepository repo;

    public PasswordResetCleanup(PasswordResetTokenRepository repo) { this.repo = repo; }

    /**
     * Ejecuta la limpieza cada hora en la zona de Madrid.
     */
    @Transactional
    @Scheduled(cron = "0 0 * * * *", zone = "Europe/Madrid")
    public void clean() {
        int removed = repo.deleteExpired(Instant.now());
        if (removed > 0) log.info("PasswordResetCleanup: eliminados {} tokens caducados", removed);
        else log.debug("PasswordResetCleanup: nada que eliminar.");
    }
}
