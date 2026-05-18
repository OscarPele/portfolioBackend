package com.portfolioBackend.auth.password;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Repositorio de tokens de recuperacion de contrasena.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Busca el token y carga el usuario asociado en la misma consulta.
     */
    @Query("select t from PasswordResetToken t join fetch t.user where t.tokenHash = :hash")
    Optional<PasswordResetToken> findByTokenHashFetchUser(@Param("hash") String tokenHash);

    /**
     * Elimina solicitudes previas del usuario antes de generar una nueva.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PasswordResetToken t where t.user.id = :userId")
    void deleteAllByUserId(@Param("userId") long userId);

    /**
     * Borra tokens caducados o ya consumidos.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PasswordResetToken t where t.expiresAt < :cutoff or t.used = true")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
