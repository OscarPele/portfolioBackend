package com.portfolioBackend.auth.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Centraliza las reglas de registro, autenticacion y cambio de contrasena.
 */
@Service
public class UserService {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;


    public UserService(UserRepository userRepo,
                       PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    /**
     * Crea un usuario nuevo validando que username y email no existan.
     */
    @Transactional
    public User register(String username, String email, String rawPassword) {
        if (userRepo.existsByUsername(username)) {
            throw new RuntimeException("USERNAME_EXISTS");
        }
        if (userRepo.existsByEmail(email)) {
            throw new RuntimeException("EMAIL_EXISTS");
        }

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPassword));
        return userRepo.save(u);
    }

    /**
     * Autentica por username o email y exige que el correo este verificado.
     */
    @Transactional(readOnly = true)
    public User authenticate(String usernameOrEmail, String rawPassword) {
        var user = userRepo.findByUsername(usernameOrEmail)
                .or(() -> userRepo.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new RuntimeException("INVALID_CREDENTIALS"));

        if (!encoder.matches(rawPassword, user.getPasswordHash())) {
            throw new RuntimeException("INVALID_CREDENTIALS");
        }
        if (!user.isEnabled()) {
            throw new RuntimeException("EMAIL_NOT_VERIFIED");
        }

        return user;
    }

    /**
     * Recupera un usuario por id o lanza USER_NOT_FOUND.
     */
    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public User requireById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
    }

    /**
     * Cambia la contrasena comprobando antes la actual.
     */
    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword) {
        var user = requireById(id);
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("CURRENT_PASSWORD_INCORRECT");
        }
        user.setPasswordHash(encoder.encode(newPassword));
        userRepo.save(user);
    }

    /**
     * Busca usuarios por email ignorando mayusculas y minusculas.
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmailIgnoreCase(String email) {
        return userRepo.findByEmailIgnoreCase(email);
    }

    /**
     * Fuerza una nueva contrasena, usado por el flujo de recuperacion.
     */
    @Transactional
    public void forceChangePassword(Long userId, String rawPassword) {
        var user = requireById(userId);
        user.setPasswordHash(encoder.encode(rawPassword));
        userRepo.save(user);
    }
}
