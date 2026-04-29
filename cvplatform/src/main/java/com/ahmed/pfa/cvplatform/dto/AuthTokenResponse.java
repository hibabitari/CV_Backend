package com.ahmed.pfa.cvplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse d'authentification avec Refresh Token
 *
 * Retourné par:
 * - POST /api/auth/login
 * - POST /api/auth/refresh
 *
 * Contient:
 * - Access token (15 min)
 * - Refresh token (7 jours)
 * - Informations utilisateur
 * - Expiration
 *
 * @author Ahmed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthTokenResponse {

    /**
     * Message de succès
     */
    private String message;

    /**
     * ID de l'utilisateur
     */
    private Long userId;

    /**
     * Email de l'utilisateur
     */
    private String email;

    /**
     * Role de l'utilisateur (ADMIN, ETUDIANT)
     */
    private String role;

    /**
     * Access Token JWT (courte durée: 15 min)
     * Utilisé pour authentifier les requêtes API
     */
    private String accessToken;

    /**
     * Refresh Token JWT (longue durée: 7 jours)
     * Utilisé pour renouveler l'access token
     */
    private String refreshToken;

    /**
     * Durée de vie de l'access token en secondes
     * Frontend peut calculer: Date.now() + (expiresIn * 1000)
     */
    private Long expiresIn;

    /**
     * Type de token (toujours "Bearer" pour JWT)
     */
    private String tokenType = "Bearer";
}