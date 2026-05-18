package com.portfolioBackend.auth.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de tokens de verificacion de email.
 */
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    /**
     * Busca un token por su hash persistido.
     */
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    /**
     * Borra todos los tokens del usuario.
     */
    void deleteByUser_Id(Long userId);

    /**
     * Borra tokens pendientes del usuario para evitar enlaces activos duplicados.
     */
    void deleteByUser_IdAndUsedAtIsNull(Long userId);
}
