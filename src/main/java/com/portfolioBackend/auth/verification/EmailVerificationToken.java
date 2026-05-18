package com.portfolioBackend.auth.verification;

import jakarta.persistence.*;

import java.time.Instant;

import com.portfolioBackend.auth.user.User;

/**
 * Token persistido para activar cuentas mediante correo electronico.
 */
@Entity
@Table(name = "email_verification_tokens",
        indexes = @Index(name = "idx_evt_user", columnList = "user_id"))
public class EmailVerificationToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_evt_user"))
    private User user;

    /**
     * Hash del token plano (Base64Url de SHA-256).
     */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant usedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Fija la fecha de creacion antes de guardar el token.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    /**
     * Indica si el token ya fue consumido.
     */
    public boolean isUsed()    { return usedAt != null; }

    /**
     * Indica si el token ya paso su fecha de caducidad.
     */
    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
