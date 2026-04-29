package com.ahmed.pfa.cvplatform.service;

import com.ahmed.pfa.cvplatform.dto.*;
import com.ahmed.pfa.cvplatform.exception.*;
import com.ahmed.pfa.cvplatform.model.Administrateur;
import com.ahmed.pfa.cvplatform.model.Etudiant;
import com.ahmed.pfa.cvplatform.model.RefreshToken;
import com.ahmed.pfa.cvplatform.model.Utilisateur;
import com.ahmed.pfa.cvplatform.repository.AdministrateurRepository;
import com.ahmed.pfa.cvplatform.repository.EtudiantRepository;
import com.ahmed.pfa.cvplatform.repository.UtilisateurRepository;
import com.ahmed.pfa.cvplatform.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service gérant l'authentification et l'inscription des utilisateurs
 *
 * MODIFICATIONS REFRESH TOKEN:
 * - login() retourne access + refresh tokens
 * - refresh() renouvelle access token
 * - logout() révoque refresh token
 * - changePassword() révoque tous les tokens
 *
 * @author Ahmed
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private EtudiantRepository etudiantRepository;

    @Autowired
    private AdministrateurRepository administrateurRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Inscription d'un nouvel utilisateur (Étudiant ou Admin)
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("Tentative d'inscription: email={}, role={}", request.getEmail(), request.getRole());

        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            logger.warn("Inscription échouée - l'email est déjà utilisé: {}", request.getEmail());
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        String hashedPassword = passwordEncoder.encode(request.getMotDePasse());

        if ("ETUDIANT".equals(request.getRole())) {
            Etudiant etudiant = new Etudiant();
            etudiant.setNom(request.getNom());
            etudiant.setPrenom(request.getPrenom());
            etudiant.setEmail(request.getEmail());
            etudiant.setMotDePasse(hashedPassword);
            etudiant.setRole("ETUDIANT");
            etudiant.setTypeUtilisateur("ETUDIANT");
            etudiant.setDateCreation(LocalDateTime.now());
            etudiant.setNiveauEtude(request.getNiveauEtude());
            etudiant.setDomaineEtude(request.getDomaineEtude());
            etudiant.setUniversite(request.getUniversite());

            Etudiant saved = etudiantRepository.save(etudiant);
            logger.info("Étudiant créé avec succès: id={}, email={}", saved.getId(), saved.getEmail());

            return new AuthResponse("Étudiant créé avec succès", saved.getId(), saved.getEmail());

        } else if ("ADMIN".equals(request.getRole())) {
            Administrateur admin = new Administrateur();
            admin.setNom(request.getNom());
            admin.setPrenom(request.getPrenom());
            admin.setEmail(request.getEmail());
            admin.setMotDePasse(hashedPassword);
            admin.setRole("ADMIN");
            admin.setTypeUtilisateur("ADMIN");
            admin.setDateCreation(LocalDateTime.now());
            // Admin possède toutes les permissions + initialiser dernière connexion
            admin.setPermissions("ALL");
            admin.setDateDerniereConnexion(LocalDateTime.now());

            Administrateur saved = administrateurRepository.save(admin);
            logger.info("Administrateur créé avec succès: id={}, email={}", saved.getId(), saved.getEmail());

            return new AuthResponse("Administrateur créé avec succès", saved.getId(), saved.getEmail());

        } else {
            logger.error("Rôle invalide fourni lors de l'inscription: {}", request.getRole());
            throw new IllegalArgumentException("Rôle non valide. Utilisez ETUDIANT ou ADMIN");
        }
    }

    /**
     * Authentification de l'utilisateur et génération des tokens (Access + Refresh)
     *
     * MODIFICATION REFRESH TOKEN:
     * - Génère Access Token (15 min)
     * - Génère et sauvegarde Refresh Token (7 jours)
     * - Retourne les deux tokens
     */
    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        logger.debug("Tentative de connexion: email={}", request.getEmail());

        // 1. Récupérer utilisateur
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail());

        if (utilisateur == null) {
            logger.warn("Connexion échouée - Utilisateur non trouvé: {}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        // 2. Vérifier mot de passe
        if (!passwordEncoder.matches(request.getMotDePasse(), utilisateur.getMotDePasse())) {
            logger.warn("Connexion échouée - Mot de passe incorrect: {}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        // 2bis. Mettre à jour la dernière connexion (ADMIN) et garantir permissions
        if (utilisateur instanceof Administrateur admin) {
            admin.setDateDerniereConnexion(LocalDateTime.now());
            if (admin.getPermissions() == null || admin.getPermissions().isBlank()) {
                admin.setPermissions("ALL");
            }
            administrateurRepository.save(admin);
        }

        // 3. Générer Access Token (15 min)
        String accessToken = jwtUtil.generateAccessToken(
                utilisateur.getEmail(),
                utilisateur.getId(),
                utilisateur.getRole(),
                utilisateur.getNom(),
                utilisateur.getPrenom()
        );

        // 4. Créer et sauvegarder Refresh Token (7 jours)
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(utilisateur);

        logger.info("Connexion réussie: userId={}, email={}, role={}",
                utilisateur.getId(), utilisateur.getEmail(), utilisateur.getRole());

        // 5. Retourner response avec les 2 tokens
        return AuthTokenResponse.builder()
                .message("Connexion réussie")
                .userId(utilisateur.getId())
                .email(utilisateur.getEmail())
                .role(utilisateur.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtUtil.getAccessTokenExpirationInSeconds())
                .tokenType("Bearer")
                .build();
    }

    /**
     * Renouvelle l'access token à partir d'un refresh token valide
     *
     * NOUVEAU: Refresh Token Flow
     *
     * @param request Contient le refresh token
     * @return Nouveau access token (+ optionnellement nouveau refresh token si rotation)
     */
    @Transactional
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        logger.info("Demande de renouvellement de token");

        // 1. Valider refresh token
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(
                request.getRefreshToken()
        );

        Utilisateur utilisateur = refreshToken.getUtilisateur();

        // 2. Générer nouveau access token
        String newAccessToken = jwtUtil.generateAccessToken(
                utilisateur.getEmail(),
                utilisateur.getId(),
                utilisateur.getRole(),
                utilisateur.getNom(),
                utilisateur.getPrenom()
        );

        logger.info("Access token renouvelé pour userId={}", utilisateur.getId());

        // 3. Option: Rotation du refresh token (sécurité renforcée)
        // Pour l'instant, on garde le même refresh token
        // Si tu veux activer rotation, décommente ci-dessous:

        /*
        // Révoquer ancien refresh token
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());

        // Créer nouveau refresh token
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(utilisateur);

        return AuthTokenResponse.builder()
            .message("Token renouvelé avec rotation")
            .userId(utilisateur.getId())
            .email(utilisateur.getEmail())
            .role(utilisateur.getRole())
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken.getToken())  // Nouveau refresh token
            .expiresIn(jwtUtil.getAccessTokenExpirationInSeconds())
            .tokenType("Bearer")
            .build();
        */

        // 4. Retourner nouveau access token (refresh token inchangé)
        return AuthTokenResponse.builder()
                .message("Token renouvelé avec succès")
                .userId(utilisateur.getId())
                .email(utilisateur.getEmail())
                .role(utilisateur.getRole())
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())  // Même refresh token
                .expiresIn(jwtUtil.getAccessTokenExpirationInSeconds())
                .tokenType("Bearer")
                .build();
    }

    /**
     * Déconnexion - révoque le refresh token
     *
     * NOUVEAU: Logout réel
     *
     * @param refreshTokenString Refresh token à révoquer
     */
    @Transactional
    public void logout(String refreshTokenString) {
        logger.info("Demande de déconnexion");

        refreshTokenService.revokeRefreshToken(refreshTokenString);

        logger.info("Déconnexion réussie - Refresh token révoqué");
    }

    /**
     * Change le mot de passe de l'utilisateur connecté
     *
     * MODIFICATION REFRESH TOKEN:
     * - Révoque TOUS les refresh tokens de l'utilisateur
     * - Force re-login sur tous les devices
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        logger.info("Demande changement mot de passe: userId={}", userId);

        // 1. Vérifier confirmation
        if (!request.getNouveauMotDePasse().equals(request.getConfirmationMotDePasse())) {
            logger.warn("Changement mot de passe échoué - confirmation différente: userId={}", userId);
            throw new PasswordMismatchException("Les mots de passe ne correspondent pas");
        }

        // 2. Récupérer utilisateur
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));

        // 3. Vérifier ancien mot de passe
        if (!passwordEncoder.matches(request.getAncienMotDePasse(), utilisateur.getMotDePasse())) {
            logger.warn("Changement mot de passe échoué - ancien incorrect: userId={}", userId);
            throw new InvalidCredentialsException("L'ancien mot de passe est incorrect");
        }

        // 4. Vérifier nouveau != ancien
        if (passwordEncoder.matches(request.getNouveauMotDePasse(), utilisateur.getMotDePasse())) {
            logger.warn("Changement mot de passe échoué - nouveau identique: userId={}", userId);
            throw new SamePasswordException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        // 5. Mettre à jour mot de passe
        String hashedPassword = passwordEncoder.encode(request.getNouveauMotDePasse());
        utilisateur.setMotDePasse(hashedPassword);
        utilisateurRepository.save(utilisateur);

        // 6. IMPORTANT: Révoquer tous les refresh tokens (force re-login partout)
        int revokedCount = refreshTokenService.revokeAllUserTokens(userId);

        logger.info("Mot de passe changé avec succès: userId={}, {} tokens révoqués",
                userId, revokedCount);
    }

    /**
     * Vérifie qu'un compte étudiant existe pour la réinitialisation du mot de passe (sans token).
     */
    public void verifyStudentEmailForPasswordReset(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email);
        if (utilisateur == null || !"ETUDIANT".equals(utilisateur.getRole())) {
            logger.warn("Mot de passe oublié - email inconnu ou non étudiant: {}", email);
            throw new ResourceNotFoundException("Aucun compte étudiant trouvé avec cet email.");
        }
    }

    /**
     * Réinitialise le mot de passe d'un étudiant après vérification email (sans authentification).
     * Révoque tous les refresh tokens comme un changement de mot de passe classique.
     */
    @Transactional
    public void resetStudentPasswordByEmail(ForgotPasswordResetRequest request) {
        if (!request.getNouveauMotDePasse().equals(request.getConfirmationMotDePasse())) {
            logger.warn("Réinitialisation mot de passe - confirmation différente pour email={}", request.getEmail());
            throw new PasswordMismatchException("Les mots de passe ne correspondent pas");
        }

        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail());
        if (utilisateur == null || !"ETUDIANT".equals(utilisateur.getRole())) {
            logger.warn("Réinitialisation mot de passe - email invalide ou non étudiant: {}", request.getEmail());
            throw new ResourceNotFoundException("Aucun compte étudiant trouvé avec cet email.");
        }

        String hashedPassword = passwordEncoder.encode(request.getNouveauMotDePasse());
        utilisateur.setMotDePasse(hashedPassword);
        utilisateurRepository.save(utilisateur);

        int revokedCount = refreshTokenService.revokeAllUserTokens(utilisateur.getId());
        logger.info("Mot de passe réinitialisé (oubli): userId={}, {} tokens révoqués",
                utilisateur.getId(), revokedCount);
    }
}
