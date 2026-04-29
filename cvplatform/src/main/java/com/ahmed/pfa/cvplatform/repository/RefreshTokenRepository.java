package com.ahmed.pfa.cvplatform.repository;

import com.ahmed.pfa.cvplatform.model.RefreshToken;
import com.ahmed.pfa.cvplatform.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des Refresh Tokens
 *
 * Fonctionnalités:
 * - CRUD refresh tokens
 * - Validation et révocation
 * - Cleanup automatique
 * - Gestion multi-device
 *
 * @author Ahmed
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Trouve un refresh token par sa valeur
     * Utilisé pour validation lors du refresh
     *
     * @param token Valeur du token JWT
     * @return Optional<RefreshToken>
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Trouve tous les refresh tokens VALIDES d'un utilisateur
     * Valide = non révoqué ET non expiré
     *
     * @param utilisateur Utilisateur
     * @return Liste des tokens valides
     */
    @Query("SELECT rt FROM RefreshToken rt " +
            "WHERE rt.utilisateur = :utilisateur " +
            "AND rt.revoked = false " +
            "AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUser(
            @Param("utilisateur") Utilisateur utilisateur,
            @Param("now") LocalDateTime now
    );

    /**
     * Trouve tous les refresh tokens d'un utilisateur (actifs + révoqués)
     * Utile pour afficher historique sessions
     *
     * @param utilisateur Utilisateur
     * @return Liste de tous les tokens
     */
    List<RefreshToken> findByUtilisateur(Utilisateur utilisateur);

    /**
     * Vérifie si un token existe et n'est pas révoqué
     *
     * @param token Valeur du token
     * @return true si existe et actif
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END " +
            "FROM RefreshToken rt " +
            "WHERE rt.token = :token " +
            "AND rt.revoked = false")
    boolean existsByTokenAndNotRevoked(@Param("token") String token);

    /**
     * Révoque un token spécifique (logout)
     *
     * @param token Valeur du token
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE RefreshToken rt " +
            "SET rt.revoked = true, rt.revokedAt = :now " +
            "WHERE rt.token = :token")
    int revokeByToken(
            @Param("token") String token,
            @Param("now") LocalDateTime now
    );

    /**
     * Révoque TOUS les tokens d'un utilisateur
     * Utilisé lors du changement de mot de passe
     *
     * @param utilisateur Utilisateur
     * @param now DateTime actuel
     * @return Nombre de tokens révoqués
     */
    @Modifying
    @Query("UPDATE RefreshToken rt " +
            "SET rt.revoked = true, rt.revokedAt = :now " +
            "WHERE rt.utilisateur = :utilisateur " +
            "AND rt.revoked = false")
    int revokeAllByUser(
            @Param("utilisateur") Utilisateur utilisateur,
            @Param("now") LocalDateTime now
    );

    /**
     * Supprime les tokens expirés depuis plus de X jours
     * Utilisé par le scheduled task de cleanup
     *
     * @param date Date limite (ex: now - 30 jours)
     * @return Nombre de tokens supprimés
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt " +
            "WHERE rt.expiresAt < :date")
    int deleteExpiredTokens(@Param("date") LocalDateTime date);

    /**
     * Compte le nombre de tokens actifs d'un utilisateur
     * Utile pour limiter le nombre de devices connectés
     *
     * @param utilisateur Utilisateur
     * @param now DateTime actuel
     * @return Nombre de tokens actifs
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt " +
            "WHERE rt.utilisateur = :utilisateur " +
            "AND rt.revoked = false " +
            "AND rt.expiresAt > :now")
    long countActiveTokensByUser(
            @Param("utilisateur") Utilisateur utilisateur,
            @Param("now") LocalDateTime now
    );
}

