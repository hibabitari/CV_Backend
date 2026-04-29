package com.ahmed.pfa.cvplatform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity représentant un Refresh Token JWT
 *
 * Permet:
 * - Authentification durable (7 jours)
 * - Révocation sur logout/password change
 * - Audit trail (IP, user agent)
 * - Cleanup automatique des tokens expirés
 *
 * Design Pattern: Token Repository Pattern
 *
 * @author Ahmed
 */
@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_token", columnList = "token"),
        @Index(name = "idx_expires_at", columnList = "expires_at"),
        @Index(name = "idx_revoked", columnList = "revoked")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * JWT refresh token string (unique)
     * Longueur ~400-500 chars
     */
    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    /**
     * Utilisateur propriétaire du token
     * Relation Many-to-One (un user peut avoir plusieurs refresh tokens)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_refresh_token_user"))
    private Utilisateur utilisateur;

    /**
     * Date de création du token
     * Auto-set par @PrePersist ou DEFAULT CURRENT_TIMESTAMP
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Date d'expiration du token
     * Par défaut: createdAt + 7 jours
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Indique si le token a été révoqué
     * true = logout, password change, ou révocation manuelle
     */
    @Column(name = "revoked", nullable = false)
    private Boolean revoked = false;

    /**
     * Date de révocation (si revoked = true)
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * Adresse IP lors de la création du token
     * Utile pour détection activités suspectes
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User Agent (browser/device) lors de la création
     * Utile pour afficher "Connecté depuis Chrome, Windows"
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Auto-set createdAt lors de la persistance
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Vérifie si le token est expiré
     *
     * @return true si expiré, false sinon
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Vérifie si le token est valide
     * Un token est valide si:
     * - Non révoqué
     * - Non expiré
     *
     * @return true si valide, false sinon
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }

    /**
     * Révoque le token
     * Appelé lors du logout ou password change
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = LocalDateTime.now();
    }
}