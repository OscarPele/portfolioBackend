package com.portfolioBackend.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso persistente a usuarios y busquedas usadas por autenticacion.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
}
