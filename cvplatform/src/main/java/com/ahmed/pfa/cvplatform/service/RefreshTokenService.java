package com.ahmed.pfa.cvplatform.service;

import com.ahmed.pfa.cvplatform.exception.ExpiredRefreshTokenException;
import com.ahmed.pfa.cvplatform.exception.InvalidRefreshTokenException;
import com.ahmed.pfa.cvplatform.exception.ResourceNotFoundException;
import com.ahmed.pfa.cvplatform.model.RefreshToken;
import com.ahmed.pfa.cvplatform.model.Utilisateur;
import com.ahmed.pfa.cvplatform.repository.RefreshTokenRepository;
import com.ahmed.pfa.cvplatform.repository.UtilisateurRepository;
import com.ahmed.pfa.cvplatform.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de gestion des Refresh Tokens
 *
 * Fonctionnalités:
 * - Création refresh token lors du login
 * - Validation et renouvellement (refresh flow)
 * - Révocation (logout)
 * - Révocation globale (password change)
 * - Cleanup automatique (scheduled task)
 *
 * Design Pattern: Token Repository Pattern
 *
 * @author Ahmed
 */
@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenExpiration;

    /**
     * Crée et sauvegarde un nouveau refresh token pour un utilisateur
     *
     * Appelé lors du login
     *
     * @param utilisateur Utilisateur pour lequel créer le token
     * @return RefreshToken entity créé et sauvegardé
     */
    @Transactional
    public RefreshToken createRefreshToken(Utilisateur utilisateur) {
        logger.info("Création refresh token pour userId={}", utilisateur.getId());

        // 1. Générer le token JWT
        String tokenString = jwtUtil.generateRefreshToken(
                utilisateur.getEmail(),
                utilisateur.getId()
        );

        // 2. Calculer date d'expiration
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(refreshTokenExpiration / 1000);

        // 3. Extraire IP et User Agent (audit trail)
        String ipAddress = extractIpAddress();
        String userAgent = extractUserAgent();

        // 4. Créer entity
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenString)
                .utilisateur(utilisateur)
                .expiresAt(expiresAt)
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        // 5. Sauvegarder en DB
        RefreshToken saved = refreshTokenRepository.save(refreshToken);

        logger.info("Refresh token créé: id={}, userId={}, expiresAt={}",
                saved.getId(), utilisateur.getId(), expiresAt);

        return saved;
    }

    /**
     * Valide un refresh token et retourne l'entity associée
     *
     * Vérifie:
     * 1. Token existe en DB
     * 2. Token pas révoqué
     * 3. Token pas expiré
     * 4. Signature JWT valide
     *
     * @param tokenString Valeur du refresh token JWT
     * @return RefreshToken entity si valide
     * @throws InvalidRefreshTokenException si token invalide
     * @throws ExpiredRefreshTokenException si token expiré
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String tokenString) {
        logger.debug("Validation refresh token");

        // 1. Chercher token en DB
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenString)
                .orElseThrow(() -> {
                    logger.warn("Refresh token non trouvé en DB");
                    return new InvalidRefreshTokenException("Refresh token invalide");
                });

        // 2. Vérifier si révoqué
        if (refreshToken.getRevoked()) {
            logger.warn("Refresh token révoqué: id={}, revokedAt={}",
                    refreshToken.getId(), refreshToken.getRevokedAt());
            throw new InvalidRefreshTokenException("Refresh token révoqué");
        }

        // 3. Vérifier si expiré
        if (refreshToken.isExpired()) {
            logger.warn("Refresh token expiré: id={}, expiresAt={}",
                    refreshToken.getId(), refreshToken.getExpiresAt());
            throw new ExpiredRefreshTokenException("Refresh token expiré, veuillez vous reconnecter");
        }

        // 4. Valider signature JWT
        String email = refreshToken.getUtilisateur().getEmail();
        if (!jwtUtil.validateRefreshToken(tokenString, email)) {
            logger.warn("Signature refresh token invalide: userId={}",
                    refreshToken.getUtilisateur().getId());
            throw new InvalidRefreshTokenException("Signature refresh token invalide");
        }

        logger.debug("Refresh token valide: id={}, userId={}",
                refreshToken.getId(), refreshToken.getUtilisateur().getId());

        return refreshToken;
    }

    /**
     * Révoque un refresh token spécifique (logout)
     *
     * @param tokenString Valeur du refresh token
     */
    @Transactional
    public void revokeRefreshToken(String tokenString) {
        logger.info("Révocation refresh token");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token non trouvé"));

        if (!refreshToken.getRevoked()) {
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);

            logger.info("Refresh token révoqué: id={}, userId={}",
                    refreshToken.getId(), refreshToken.getUtilisateur().getId());
        } else {
            logger.debug("Refresh token déjà révoqué: id={}", refreshToken.getId());
        }
    }

    /**
     * Révoque TOUS les refresh tokens d'un utilisateur
     *
     * Utilisé lors du changement de mot de passe
     * Force déconnexion de tous les devices
     *
     * @param userId ID de l'utilisateur
     * @return Nombre de tokens révoqués
     */
    @Transactional
    public int revokeAllUserTokens(Long userId) {
        logger.info("Révocation de tous les tokens pour userId={}", userId);

        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));

        int revokedCount = refreshTokenRepository.revokeAllByUser(
                utilisateur,
                LocalDateTime.now()
        );

        logger.info("{} refresh tokens révoqués pour userId={}", revokedCount, userId);

        return revokedCount;
    }

    /**
     * Supprime un refresh token après révocation (cleanup immédiat)
     *
     * Optionnel: permet de nettoyer immédiatement au lieu d'attendre scheduled task
     *
     * @param tokenString Valeur du refresh token
     */
    @Transactional
    public void deleteRefreshToken(String tokenString) {
        logger.debug("Suppression refresh token");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token non trouvé"));

        refreshTokenRepository.delete(refreshToken);

        logger.info("Refresh token supprimé: id={}", refreshToken.getId());
    }

    /**
     * Compte le nombre de tokens actifs pour un utilisateur
     *
     * Utile pour:
     * - Limiter le nombre de devices connectés
     * - Dashboard admin (sessions actives)
     *
     * @param userId ID de l'utilisateur
     * @return Nombre de tokens actifs (non révoqués, non expirés)
     */
    @Transactional(readOnly = true)
    public long countActiveTokens(Long userId) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));

        long count = refreshTokenRepository.countActiveTokensByUser(
                utilisateur,
                LocalDateTime.now()
        );

        logger.debug("Nombre de tokens actifs pour userId={}: {}", userId, count);

        return count;
    }

    /**
     * Liste tous les refresh tokens d'un utilisateur (actifs + révoqués)
     *
     * Utile pour dashboard "Gérer mes sessions"
     *
     * @param userId ID de l'utilisateur
     * @return Liste des refresh tokens
     */
    @Transactional(readOnly = true)
    public List<RefreshToken> getUserTokens(Long userId) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));

        return refreshTokenRepository.findByUtilisateur(utilisateur);
    }

    /**
     * Cleanup automatique des tokens expirés
     *
     * Scheduled task: Exécuté quotidiennement à 3h du matin
     * Supprime les tokens expirés depuis plus de 30 jours
     *
     * Évite accumulation en DB
     */
    @Scheduled(cron = "0 0 3 * * *")  // Tous les jours à 3h
    @Transactional
    public void cleanupExpiredTokens() {
        logger.info("Début cleanup refresh tokens expirés");

        // Supprimer tokens expirés depuis plus de 30 jours
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        int deletedCount = refreshTokenRepository.deleteExpiredTokens(cutoffDate);

        logger.info("Cleanup terminé: {} tokens supprimés", deletedCount);
    }

    /**
     * Extrait l'adresse IP de la requête HTTP courante
     *
     * @return IP address ou null si indisponible
     */
    private String extractIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // Vérifier headers proxy (X-Forwarded-For, X-Real-IP)
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }

                return ip;
            }
        } catch (Exception e) {
            logger.warn("Impossible d'extraire IP address: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Extrait le User Agent de la requête HTTP courante
     *
     * @return User Agent string ou null si indisponible
     */
    private String extractUserAgent() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            logger.warn("Impossible d'extraire User Agent: {}", e.getMessage());
        }

        return null;
    }
}