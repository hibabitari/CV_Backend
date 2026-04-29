package com.ahmed.pfa.cvplatform.controller;

import com.ahmed.pfa.cvplatform.dto.*;
import com.ahmed.pfa.cvplatform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication REST Controller
 *
 * Handles user authentication, registration, and token management.
 *
 * FEATURES:
 * - User registration (ETUDIANT role)
 * - Login with JWT access + refresh tokens
 * - Token refresh mechanism
 * - Logout with token revocation
 * - Password change with global token revocation
 * - Rate limiting on login (5 attempts/minute)
 *
 * @author Ahmed
 */
@Tag(name = "Authentication", description = "Endpoints d'authentification et gestion des tokens JWT")
@RestController
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * Register a new student account
     *
     * POST /api/auth/register
     */
    @Operation(
            summary = "Créer un nouveau compte étudiant",
            description = """
            Inscription d'un nouvel étudiant avec validation du mot de passe.
            
            **Règles de validation mot de passe:**
            - Minimum 8 caractères
            - Au moins 1 majuscule
            - Au moins 1 minuscule
            - Au moins 1 chiffre
            - Au moins 1 caractère spécial (@$!%*?&#)
            
            **Rôle assigné:** ETUDIANT (par défaut)
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Étudiant créé avec succès"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données invalides (validation échouée)"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email déjà utilisé"
            )
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Requête d'inscription reçue: email={}", request.getEmail());

        AuthResponse response = authService.register(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Login and get JWT tokens
     *
     * POST /api/auth/login
     */
    @Operation(
            summary = "Se connecter et obtenir les tokens JWT",
            description = """
            Authentification avec email/password. Retourne deux tokens:
            
            **Access Token:**
            - Durée: 15 minutes
            - Usage: Authentification sur tous les endpoints protégés
            - À inclure dans header: `Authorization: Bearer {accessToken}`
            
            **Refresh Token:**
            - Durée: 7 jours
            - Usage: Renouveler l'access token via `/api/auth/refresh`
            - Stocké côté serveur (révocable)
            
            **Rate Limiting:** 5 tentatives par minute par IP.
            Au-delà, retourne 429 Too Many Requests.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Connexion réussie - Access token et refresh token générés"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Email ou mot de passe incorrect"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Trop de tentatives - Rate limit dépassé (5/minute)"
            )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Requête de connexion reçue: email={}", request.getEmail());

        AuthTokenResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token
     *
     * POST /api/auth/refresh
     */
    @Operation(
            summary = "Renouveler l'access token",
            description = """
            Utilise le refresh token pour obtenir un nouveau access token sans se reconnecter.
            
            **Processus:**
            1. Fournir le refresh token obtenu lors du login
            2. Si valide et non révoqué → nouveau access token généré
            3. Le refresh token reste le même (pas de rotation)
            
            **Utiliser quand:**
            - L'access token expire (après 15 minutes)
            - Erreur 401 sur un endpoint protégé
            
            **Le refresh token est révoqué si:**
            - Logout explicite
            - Changement de mot de passe
            - Expiration (7 jours)
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Token renouvelé avec succès - Nouveau access token"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token invalide, expiré ou révoqué"
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        logger.info("Requête de renouvellement de token reçue");

        AuthTokenResponse response = authService.refresh(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Logout and revoke refresh token
     *
     * POST /api/auth/logout
     */
    @Operation(
            summary = "Se déconnecter",
            description = """
            Révoque le refresh token pour empêcher son utilisation future.
            
            **Effet:**
            - Le refresh token devient inutilisable
            - L'access token reste valide jusqu'à expiration (max 15 min)
            - Il faudra se reconnecter pour obtenir de nouveaux tokens
            
            **Sécurité:**
            - Empêche la réutilisation du refresh token
            - Trace l'heure de révocation en base de données
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Déconnexion réussie - Refresh token révoqué"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token invalide"
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        logger.info("Requête de déconnexion reçue");

        authService.logout(request.getRefreshToken());

        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }

    /**
     * Change password
     *
     * POST /api/auth/change-password
     * Requires: Valid JWT access token
     */
    @Operation(
            summary = "Changer le mot de passe",
            description = """
            Modifie le mot de passe et révoque **TOUS** les refresh tokens de l'utilisateur.
            
            **Processus:**
            1. Vérification de l'ancien mot de passe
            2. Validation du nouveau mot de passe (mêmes règles que register)
            3. Vérification que nouveau ≠ ancien
            4. Vérification que nouveau = confirmation
            5. Mise à jour du mot de passe
            6. **RÉVOCATION de tous les refresh tokens** (sécurité)
            
            **⚠️ Impact sécurité:**
            - Tous les appareils connectés seront déconnectés
            - Il faudra se reconnecter partout avec le nouveau mot de passe
            
            **Authentification requise:** Access token JWT valide dans header Authorization
            """,
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Mot de passe changé avec succès - Tous refresh tokens révoqués"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation échouée ou nouveau mot de passe identique à l'ancien"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token invalide ou ancien mot de passe incorrect"
            )
    })
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = getCurrentUserId();

        authService.changePassword(userId, request);

        return ResponseEntity.ok(Map.of(
                "message", "Mot de passe changé avec succès. Veuillez vous reconnecter sur tous vos appareils."
        ));
    }

    /**
     * Étape 1 : vérifier que l'email correspond à un compte étudiant (mot de passe oublié).
     */
    @PostMapping({"/forgot-password/verify-email", "/forgot-password/verify-email/"})
    public ResponseEntity<Map<String, String>> verifyEmailForForgotPassword(
            @Valid @RequestBody ForgotPasswordVerifyEmailRequest request) {
        logger.info("Vérification email mot de passe oublié: email={}", request.getEmail());
        authService.verifyStudentEmailForPasswordReset(request.getEmail().trim());
        return ResponseEntity.ok(Map.of("message", "Email reconnu. Vous pouvez définir un nouveau mot de passe."));
    }

    /**
     * GET sur l'URL de vérification (ex. lien collé dans le navigateur) : rappel d'utiliser POST + JSON.
     */
    @GetMapping({"/forgot-password/verify-email", "/forgot-password/verify-email/"})
    public ResponseEntity<Map<String, String>> verifyEmailForForgotPasswordMethodNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.ALLOW, "POST")
                .body(Map.of(
                        "message",
                        "Cette URL n'accepte que POST (JSON {\"email\":\"...\"}). Utilisez le formulaire « Mot de passe oublié » de l'application."));
    }

    /**
     * Étape 2 : définir le nouveau mot de passe (compte étudiant uniquement).
     */
    @PostMapping({"/forgot-password/reset", "/forgot-password/reset/"})
    public ResponseEntity<Map<String, String>> resetPasswordForgot(@Valid @RequestBody ForgotPasswordResetRequest request) {
        logger.info("Réinitialisation mot de passe oublié: email={}", request.getEmail());
        authService.resetStudentPasswordByEmail(request);
        return ResponseEntity.ok(Map.of(
                "message", "Mot de passe mis à jour. Vous pouvez vous connecter."
        ));
    }

    @GetMapping({"/forgot-password/reset", "/forgot-password/reset/"})
    public ResponseEntity<Map<String, String>> resetPasswordForgotGetNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.ALLOW, "POST")
                .body(Map.of(
                        "message",
                        "Cette URL n'accepte que POST (JSON email, nouveauMotDePasse, confirmationMotDePasse)."));
    }
}