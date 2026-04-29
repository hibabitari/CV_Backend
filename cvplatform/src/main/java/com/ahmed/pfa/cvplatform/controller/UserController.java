package com.ahmed.pfa.cvplatform.controller;

import com.ahmed.pfa.cvplatform.dto.PagedResponse;
import com.ahmed.pfa.cvplatform.dto.UpdateProfileRequest;
import com.ahmed.pfa.cvplatform.dto.UserProfileResponse;
import com.ahmed.pfa.cvplatform.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur gérant les opérations liées aux utilisateurs
 */
@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    // =========================================================================
    // CONSULTATION ET MISE À JOUR
    // =========================================================================

    /**
     * Récupérer le profil d'un utilisateur par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long id) {
        logger.info("Utilisateur {} récupère le profil ID: {}", getAuthenticatedUserEmail(), id);
        UserProfileResponse profile = userService.getUserProfile(id);
        return ResponseEntity.ok(profile);
    }

    /**
     * Mettre à jour le profil de l'utilisateur
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request) {
        logger.info("Mise à jour du profil pour l'ID: {} demandée par {}", id, getAuthenticatedUserEmail());
        UserProfileResponse updated = userService.updateProfile(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Supprimer un utilisateur
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        logger.warn("Suppression de l'utilisateur ID: {} par l'administrateur {}", id, getAuthenticatedUserEmail());
        userService.deleteUserAsAdmin(getCurrentUserId(), id);
        return ResponseEntity.noContent().build(); // Retourne 204 No Content
    }

    // =========================================================================
    // GESTION DE LA PAGINATION (Optimisée avec PagedResponse)
    // =========================================================================

    /**
     * Récupérer tous les utilisateurs avec pagination
     * Utilise PagedResponse pour une structure JSON stable et sans avertissements
     */
    @GetMapping("/page")
    public ResponseEntity<PagedResponse<UserProfileResponse>> getAllUsersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("Demande de liste paginée - Page: {}, Taille: {}", page, size);

        // Validation et limitation de la taille de page pour la sécurité
        if (size > 100) {
            logger.warn("Taille demandée {} trop élevée, réduction à 100", size);
            size = 100;
        }
        if (size < 1) {
            size = 20;
        }

        // Récupération des données via le service
        Page<UserProfileResponse> usersPage = userService.getAllUsersPage(page, size);

        // Conversion de l'objet Page de Spring en notre DTO personnalisé PagedResponse
        PagedResponse<UserProfileResponse> response = PagedResponse.of(usersPage);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // ENDPOINTS CLASSIQUES (SANS PAGINATION)
    // =========================================================================

    /**
     * Récupérer la liste complète des utilisateurs (Non recommandé pour grands volumes)
     */
    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        logger.info("Récupération de la liste complète des utilisateurs");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Point de terminaison de test pour vérifier la disponibilité du contrôleur
     */
    @GetMapping("/test")
    public String test() {
        return "Le contrôleur des utilisateurs fonctionne correctement !";
    }
}