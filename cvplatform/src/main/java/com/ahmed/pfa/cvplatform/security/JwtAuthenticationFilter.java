package com.ahmed.pfa.cvplatform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filtre JWT pour authentifier les requêtes
 *
 * FONCTIONNEMENT:
 * 1. Extrait token du header Authorization
 * 2. Valide token avec JwtUtil
 * 3. Extrait email + userId + role du token
 * 4. Crée Authentication avec role dans authorities
 * 5. Set SecurityContext
 *
 * MODIFICATION (Phase 3):
 * - Ajout extraction du role depuis JWT
 * - Création authorities avec ROLE_ prefix
 * - Ajout role dans details
 * - Permet @PreAuthorize("hasRole('ADMIN')") dans controllers
 *
 * @author Ahmed
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. Extraire le token du header Authorization
            String token = extractTokenFromRequest(request);

            if (token != null) {
                // 2. Extraire l'email du token
                String email = jwtUtil.extractEmail(token);

                // 3. Vérifier que l'utilisateur n'est pas déjà authentifié
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // 4. Valider le token
                    if (jwtUtil.validateToken(token, email)) {

                        // 5. Extraire userId ET role du token
                        Long userId = jwtUtil.extractUserId(token);
                        String role = jwtUtil.extractRole(token);  // ← AJOUT: Extraction du role

                        logger.debug("Token valide - email: {}, userId: {}, role: {}",
                                email, userId, role);

                        // 6. Créer les authorities avec ROLE_ prefix
                        // IMPORTANT: Spring Security exige le prefix "ROLE_" pour @PreAuthorize
                        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + role)  // ← MODIFICATION: ROLE_ADMIN ou ROLE_ETUDIANT
                        );

                        // 7. Créer authentication token
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                email,      // Principal (email)
                                null,       // Credentials (pas besoin, déjà authentifié)
                                authorities // Roles avec ROLE_ prefix
                        );

                        // 8. IMPORTANT: Ajouter userId, email et role dans details
                        // Ceci permet getCurrentUserId() et autres helpers dans BaseController
                        Map<String, Object> details = new HashMap<>();
                        details.put("userId", userId);
                        details.put("email", email);
                        details.put("role", role);  // ← AJOUT: Role dans details

                        authToken.setDetails(details);

                        // 9. Mettre dans SecurityContext
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        logger.debug("Utilisateur authentifié: email={}, userId={}, role={}",
                                email, userId, role);
                    } else {
                        logger.warn("Token invalide pour: {}", email);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'authentification JWT: {}", e.getMessage(), e);
            // Continue filter chain même en cas d'erreur
            // SecurityContext reste vide → request sera rejetée si endpoint protégé
        }

        // 10. Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }

    /**
     * Extrait le token du header Authorization
     *
     * Format attendu: "Bearer <token>"
     *
     * @param request HTTP request
     * @return Token JWT ou null si absent/invalide
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Enlever "Bearer "
        }

        return null;
    }
}