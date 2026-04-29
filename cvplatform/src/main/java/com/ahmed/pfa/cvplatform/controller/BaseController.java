package com.ahmed.pfa.cvplatform.controller;

import com.ahmed.pfa.cvplatform.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Controller de base avec méthodes utilitaires communes
 *
 * FONCTIONNALITÉS:
 * - getAuthenticatedUserEmail(): Email de l'utilisateur connecté
 * - getCurrentUserId(): ID de l'utilisateur connecté (FIXÉ - Fallback JWT)
 * - isAuthenticated(): Vérification authentication
 * - getAuthentication(): Objet Authentication complet
 *
 * HÉRITAGE:
 * Tous les controllers extends BaseController
 *
 * @author Ahmed
 */
public abstract class BaseController {

    private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Récupère l'email de l'utilisateur authentifié
     *
     * @return Email de l'utilisateur ou null si non authentifié
     */
    protected String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String
                && authentication.getPrincipal().equals("anonymousUser"))) {
            return authentication.getName();
        }

        return null;
    }

    /**
     * Récupère l'ID de l'utilisateur connecté depuis le JWT
     *
     * SÉCURITÉ:
     * - userId extrait du JWT validé (pas du request body)
     * - Set par JwtAuthenticationFilter dans authentication details
     * - Impossible à falsifier (token signé)
     *
     * STRATÉGIE DE FIX:
     * 1. Tenter extraction depuis Authentication details (Map)
     * 2. Si échec, extraire directement depuis JWT token (Fallback)
     *
     * USE CASE:
     * - Filtrer "MES" ressources (offres, CVs, etc.)
     * - Ownership validation
     *
     * @return userId de l'utilisateur connecté
     * @throws IllegalStateException si userId non trouvé (ne devrait jamais arriver)
     */
    protected Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Vérifier que authentication existe
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        // STRATÉGIE 1: Extraire depuis details Map
        Object detailsObj = auth.getDetails();
        if (detailsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) detailsObj;
            Object userIdObj = details.get("userId");

            if (userIdObj != null) {
                Long userId = convertToLong(userIdObj);
                if (userId != null) {
                    logger.debug("UserId extrait depuis details: {}", userId);
                    return userId;
                }
            }
        }

        // STRATÉGIE 2: Fallback - extraire directement depuis JWT
        logger.debug("UserId non trouvé dans details, extraction depuis JWT token");
        String token = extractTokenFromRequest();

        if (token != null) {
            try {
                Long userId = jwtUtil.extractUserId(token);
                if (userId != null) {
                    logger.debug("UserId extrait depuis JWT: {}", userId);
                    return userId;
                }
            } catch (Exception e) {
                logger.error("Erreur extraction userId depuis JWT: {}", e.getMessage());
            }
        }

        // Si toutes les stratégies échouent
        String email = auth.getName();
        logger.error("Impossible d'extraire userId. Email: {}", email);
        throw new IllegalStateException("User ID introuvable pour: " + email);
    }

    /**
     * Vérifie si un utilisateur est authentifié
     *
     * @return true si authentifié, false sinon
     */
    protected boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String
                && authentication.getPrincipal().equals("anonymousUser"));
    }

    /**
     * Indique si l'utilisateur connecté a le rôle ADMIN (authority ROLE_ADMIN).
     */
    protected boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Récupère l'objet Authentication complet
     *
     * @return Authentication object ou null
     */
    protected Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Extrait une valeur des details de l'authentication
     *
     * Méthode helper générique pour extraire n'importe quelle valeur
     * stockée dans authentication details
     *
     * @param key Clé à extraire
     * @param type Type attendu
     * @param <T> Type générique
     * @return Valeur ou null si non trouvée
     */
    @SuppressWarnings("unchecked")
    protected <T> T getAuthenticationDetail(String key, Class<T> type) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getDetails() instanceof Map) {
            Map<String, Object> details = (Map<String, Object>) auth.getDetails();
            Object value = details.get(key);

            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
        }

        return null;
    }

    /**
     * Extrait le token JWT depuis le header Authorization de la requête
     *
     * @return Token JWT ou null si absent
     */
    private String extractTokenFromRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            logger.warn("RequestAttributes non disponibles");
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * Convertit un Object en Long (supporte Integer, Long, String)
     *
     * @param obj Objet à convertir
     * @return Long ou null si conversion impossible
     */
    private Long convertToLong(Object obj) {
        if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        } else if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                logger.warn("Impossible de convertir String en Long: {}", obj);
                return null;
            }
        }
        return null;
    }
}