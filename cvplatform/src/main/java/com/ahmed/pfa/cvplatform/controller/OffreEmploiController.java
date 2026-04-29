package com.ahmed.pfa.cvplatform.controller;

import com.ahmed.pfa.cvplatform.dto.OffreEmploiRequest;
import com.ahmed.pfa.cvplatform.dto.OffreEmploiResponse;
import com.ahmed.pfa.cvplatform.dto.PagedResponse;
import com.ahmed.pfa.cvplatform.service.OffreEmploiService;
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
 * Contrôleur gérant les offres d'emploi
 */
@RestController
@RequestMapping("/api/jobs")
public class OffreEmploiController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(OffreEmploiController.class);

    @Autowired
    private OffreEmploiService offreEmploiService;

    // =========================================================================
    // CRÉATION ET MISE À JOUR
    // =========================================================================

    /**
     * Créer une nouvelle offre d'emploi
     */
    @PostMapping
    public ResponseEntity<OffreEmploiResponse> createOffre(@Valid @RequestBody OffreEmploiRequest request) {
        logger.info("L'utilisateur {} crée une nouvelle offre : {}", getAuthenticatedUserEmail(), request.getTitre());
        Long uid = getCurrentUserId();
        boolean admin = isCurrentUserAdmin();
        OffreEmploiResponse response = offreEmploiService.createOffre(request, uid, admin);
        return ResponseEntity.ok(response);
    }

    /**
     * Mettre à jour une offre existante
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OffreEmploiResponse> updateOffre(
            @PathVariable Long id,
            @Valid @RequestBody OffreEmploiRequest request) {
        logger.info("Mise à jour de l'offre ID : {} par l'utilisateur {}", id, getAuthenticatedUserEmail());
        Long uid = getCurrentUserId();
        boolean admin = isCurrentUserAdmin();
        OffreEmploiResponse response = offreEmploiService.updateOffre(id, request, uid, admin);
        return ResponseEntity.ok(response);
    }

    /**
     * Désactiver une offre d'emploi sans la supprimer
     */
    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OffreEmploiResponse> desactiverOffre(@PathVariable Long id) {
        logger.warn("Désactivation de l'offre ID : {} par {}", id, getAuthenticatedUserEmail());
        Long uid = getCurrentUserId();
        boolean admin = isCurrentUserAdmin();
        OffreEmploiResponse response = offreEmploiService.desactiverOffre(id, uid, admin);
        return ResponseEntity.ok(response);
    }

    /**
     * Supprimer définitivement une offre d'emploi
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOffre(@PathVariable Long id) {
        logger.error("Suppression définitive de l'offre ID : {} par l'administrateur {}", id, getAuthenticatedUserEmail());
        Long uid = getCurrentUserId();
        boolean admin = isCurrentUserAdmin();
        offreEmploiService.deleteOffre(id, uid, admin);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // ENDPOINTS AVEC PAGINATION (OPTIMISÉS VIA PAGEDRESPONSE)
    // =========================================================================

    /**
     * Récupérer toutes les offres actives avec pagination
     */
    @GetMapping("/active/page")
    public ResponseEntity<PagedResponse<OffreEmploiResponse>> getAllOffresActivesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = validatePageSize(size);
        logger.info("Récupération des offres actives - Page : {}, Taille : {}", page, size);

        Long uid = getCurrentUserId();
        boolean admin = isCurrentUserAdmin();
        Page<OffreEmploiResponse> offresPage = offreEmploiService.getAllOffresActivesPage(page, size, uid, admin);
        return ResponseEntity.ok(PagedResponse.of(offresPage));
    }

    /**
     * Récupérer toutes les offres (actives et inactives) avec pagination
     */
    @GetMapping("/page")
    public ResponseEntity<PagedResponse<OffreEmploiResponse>> getAllOffresPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = validatePageSize(size);
        logger.info("Récupération globale des offres - Page : {}, Taille : {}", page, size);

        Long uid = getCurrentUserId();
        boolean admin = isCurrentUserAdmin();
        Page<OffreEmploiResponse> offresPage = offreEmploiService.getAllOffresPage(page, size, uid, admin);
        return ResponseEntity.ok(PagedResponse.of(offresPage));
    }

    /**
     * Filtrer les offres par localisation avec pagination
     */
    @GetMapping("/localisation/{localisation}/page")
    public ResponseEntity<PagedResponse<OffreEmploiResponse>> getOffresByLocalisationPage(
            @PathVariable String localisation,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = validatePageSize(size);
        logger.info("Recherche paginée par localisation : {} - Page : {}", localisation, page);

        Long uid = getCurrentUserId();
        boolean admin = isCurrentUserAdmin();
        Page<OffreEmploiResponse> offresPage = offreEmploiService.getOffresByLocalisationPage(localisation, page, size, uid, admin);
        return ResponseEntity.ok(PagedResponse.of(offresPage));
    }

    /**
     * Filtrer les offres par type de contrat avec pagination
     */
    @GetMapping("/contrat/{typeContrat}/page")
    public ResponseEntity<PagedResponse<OffreEmploiResponse>> getOffresByTypeContratPage(
            @PathVariable String typeContrat,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = validatePageSize(size);
        logger.info("Recherche paginée par type de contrat : {} - Page : {}", typeContrat, page);

        Long uid = getCurrentUserId();
        boolean admin = isCurrentUserAdmin();
        Page<OffreEmploiResponse> offresPage = offreEmploiService.getOffresByTypeContratPage(typeContrat, page, size, uid, admin);
        return ResponseEntity.ok(PagedResponse.of(offresPage));
    }

    // =========================================================================
    // MÉTHODE PRIVÉE DE VALIDATION
    // =========================================================================

    private int validatePageSize(int size) {
        if (size > 100) {
            logger.warn("Taille de page {} dépasse la limite, ramenée à 100", size);
            return 100;
        }
        if (size < 1) return 20;
        return size;
    }

    // =========================================================================
    // ENDPOINTS CLASSIQUES (SANS PAGINATION)
    // =========================================================================

    @GetMapping("/active")
    public ResponseEntity<List<OffreEmploiResponse>> getAllOffresActives() {
        Long uid = getCurrentUserId();
        boolean admin = isCurrentUserAdmin();
        return ResponseEntity.ok(offreEmploiService.getAllOffresActives(uid, admin));
    }

    @GetMapping("/search")
    public ResponseEntity<List<OffreEmploiResponse>> searchOffres(@RequestParam String keyword) {
        logger.info("Recherche par mot-clé : {}", keyword);
        Long uid = getCurrentUserId();
        boolean admin = isCurrentUserAdmin();
        return ResponseEntity.ok(offreEmploiService.searchOffres(keyword, uid, admin));
    }

    /**
     * Administrateur: récupérer toutes les offres créées par un étudiant (actives + inactives).
     */
    @GetMapping("/etudiant/{etudiantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OffreEmploiResponse>> getOffresByEtudiant(@PathVariable Long etudiantId) {
        return ResponseEntity.ok(offreEmploiService.getOffresByEtudiant(etudiantId));
    }

    @GetMapping("/test")
    public String test() {
        return "Le contrôleur des offres d'emploi fonctionne !";
    }

    @GetMapping("/{id}")
    public ResponseEntity<OffreEmploiResponse> getOffreById(@PathVariable Long id) {
        Long uid = getCurrentUserId();
        boolean admin = isCurrentUserAdmin();
        return ResponseEntity.ok(offreEmploiService.getOffreById(id, uid, admin));
    }
}