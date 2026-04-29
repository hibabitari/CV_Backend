package com.ahmed.pfa.cvplatform.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Utilitaire pour la gestion des tokens JWT (Access + Refresh)
 *
 * MODIFICATIONS REFRESH TOKEN:
 * - Séparation Access Token (15 min) et Refresh Token (7 jours)
 * - generateAccessToken() pour authentification courte
 * - generateRefreshToken() pour renouvellement
 * - validateRefreshToken() pour validation refresh
 *
 * @author Ahmed
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access.expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenExpiration;

    /**
     * Génère la clé secrète à partir de la configuration
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Génère un ACCESS TOKEN JWT (courte durée: 15 min)
     *
     * Contient: userId, role, email
     * Usage: Authentifier les requêtes API
     *
     * @param email Email de l'utilisateur
     * @param userId ID de l'utilisateur
     * @param role Role de l'utilisateur (ADMIN, ETUDIANT)
     * @return Access Token JWT
     */
    public String generateAccessToken(String email, Long userId, String role,  String prenom, String nom) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("prenom", prenom);
        claims.put("nom", nom);
        claims.put("type", "access");  // Type de token

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString())  // JTI unique
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        logger.debug("Access token généré: email={}, userId={}, role={}, expiration={}min",
                email, userId, role, accessTokenExpiration / 60000);

        return token;
    }

    /**
     * Génère un REFRESH TOKEN JWT (longue durée: 7 jours)
     *
     * Contient: userId, email, type=refresh
     * Usage: Renouveler l'access token sans re-login
     *
     * @param email Email de l'utilisateur
     * @param userId ID de l'utilisateur
     * @return Refresh Token JWT
     */
    public String generateRefreshToken(String email, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");  // Type de token

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString())  // JTI unique
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        logger.debug("Refresh token généré: email={}, userId={}, expiration={}jours",
                email, userId, refreshTokenExpiration / 86400000);

        return token;
    }

    /**
     * DEPRECATED: Utiliser generateAccessToken() à la place
     * Gardé pour compatibilité ascendante
     */
    @Deprecated
    public String generateToken(String email, Long userId, String role,  String prenom, String nom) {
        logger.warn("generateToken() deprecated, utiliser generateAccessToken()");
        return generateAccessToken(email, userId, role, nom, prenom);
    }

    /**
     * Extrait l'email du token
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrait l'userId du token
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            }
            return (Long) userId;
        });
    }

    /**
     * Extrait le role du token (Access token seulement)
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extrait le type du token (access ou refresh)
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    /**
     * Extrait la date d'expiration du token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrait JTI (JWT ID) du token
     */
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    /**
     * Extrait une information spécifique du token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrait tous les claims du token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Vérifie si le token a expiré
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            logger.debug("Token expiré: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Valide un ACCESS TOKEN
     */
    public boolean validateAccessToken(String token, String email) {
        try {
            final String tokenEmail = extractEmail(token);
            final String tokenType = extractTokenType(token);

            boolean isValid = tokenEmail.equals(email)
                    && !isTokenExpired(token)
                    && "access".equals(tokenType);

            if (isValid) {
                logger.debug("Access token valide: {}", email);
            } else {
                logger.warn("Access token invalide: email={}, type={}", email, tokenType);
            }

            return isValid;

        } catch (Exception e) {
            logger.error("Erreur validation access token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Valide un REFRESH TOKEN
     *
     * Vérifie:
     * - Signature correcte
     * - Email correspond
     * - Non expiré
     * - Type = refresh
     */
    public boolean validateRefreshToken(String token, String email) {
        try {
            final String tokenEmail = extractEmail(token);
            final String tokenType = extractTokenType(token);

            boolean isValid = tokenEmail.equals(email)
                    && !isTokenExpired(token)
                    && "refresh".equals(tokenType);

            if (isValid) {
                logger.debug("Refresh token valide: {}", email);
            } else {
                logger.warn("Refresh token invalide: email={}, type={}, expired={}",
                        email, tokenType, isTokenExpired(token));
            }

            return isValid;

        } catch (SignatureException e) {
            logger.error("Signature refresh token invalide: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Refresh token malformé: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("Refresh token expiré: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("Refresh token non supporté: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Refresh token claims vide: {}", e.getMessage());
        }

        return false;
    }

    /**
     * DEPRECATED: Utiliser validateAccessToken()
     * Gardé pour compatibilité
     */
    @Deprecated
    public boolean validateToken(String token, String email) {
        logger.warn("validateToken() deprecated, utiliser validateAccessToken()");
        return validateAccessToken(token, email);
    }

    /**
     * Obtient le temps restant avant expiration
     */
    public long getTimeUntilExpiration(String token) {
        try {
            Date expiration = extractExpiration(token);
            long timeRemaining = expiration.getTime() - new Date().getTime();
            return Math.max(0, timeRemaining);
        } catch (Exception e) {
            logger.error("Erreur calcul temps restant: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Obtient la durée d'expiration de l'access token (en secondes)
     * Utile pour la response "expiresIn"
     */
    public long getAccessTokenExpirationInSeconds() {
        return accessTokenExpiration / 1000;
    }

    /**
     * Obtient la durée d'expiration du refresh token (en secondes)
     */
    public long getRefreshTokenExpirationInSeconds() {
        return refreshTokenExpiration / 1000;
    }
}
