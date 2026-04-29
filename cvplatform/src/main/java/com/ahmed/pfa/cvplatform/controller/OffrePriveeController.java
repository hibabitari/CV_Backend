package com.ahmed.pfa.cvplatform.controller;

import com.ahmed.pfa.cvplatform.dto.OffrePriveeRequest;
import com.ahmed.pfa.cvplatform.dto.OffrePriveeResponse;
import com.ahmed.pfa.cvplatform.dto.PagedResponse;
import com.ahmed.pfa.cvplatform.service.OffrePriveeService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST pour la gestion des offres privées
 *
 * ARCHITECTURE:
 * - Extends BaseController (helper methods héritées pour le JWT)
 * - REST endpoints avec sécurité différenciée
 * - Admin: création, modification, stats
 * - Étudiant: consultation MES offres uniquement
 *
 * SÉCURITÉ:
 * - @PreAuthorize("hasRole('ADMIN')") pour endpoints admin
 * - Validation ownership systématique dans la couche Service
 *
 * @author Ahmed
 */
@RestController
@RequestMapping("/api/offres-privees")
public class OffrePriveeController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(OffrePriveeController.class);

    @Autowired
    private OffrePriveeService offrePriveeService;

    // ============================================
    // ENDPOINTS ADMIN (Gestion)
    // ============================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OffrePriveeResponse> createOffrePrivee(
            @Valid @RequestBody OffrePriveeRequest request,
            @RequestParam Long destinataireId) {

        String emetteurEmail = getAuthenticatedUserEmail();

        logger.info("Admin {} crée offre privée pour étudiant {}",
                emetteurEmail, destinataireId);

        OffrePriveeResponse response = offrePriveeService.createOffrePrivee(
                request,
                destinataireId,
                emetteurEmail
        );

        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OffrePriveeResponse> updateOffrePrivee(
            @PathVariable Long id,
            @Valid @RequestBody OffrePriveeRequest request) {

        logger.info("Admin {} modifie offre privée {}",
                getAuthenticatedUserEmail(), id);

        OffrePriveeResponse response = offrePriveeService.updateOffrePrivee(id, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactiverOffre(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {

        String raison = "Désactivée par admin";
        if (body != null) {
            String r = body.get("raison");
            if (r != null && !r.isBlank()) {
                raison = r.trim();
            }
        }

        logger.info("Admin {} désactive offre {}: raison={}",
                getAuthenticatedUserEmail(), id, raison);

        offrePriveeService.desactiverOffre(id, raison);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOffre(@PathVariable Long id) {

        logger.warn("Admin {} supprime définitivement offre {}",
                getAuthenticatedUserEmail(), id);

        offrePriveeService.deleteOffre(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<OffrePriveeResponse>> getAllOffresPrivees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.debug("Admin {} récupère toutes offres: page={}, size={}",
                getAuthenticatedUserEmail(), page, size);

        Page<OffrePriveeResponse> offresPage = offrePriveeService
                .getAllOffresPrivees(page, size);

        PagedResponse<OffrePriveeResponse> response = PagedResponse.of(offresPage);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getStatistics() {

        logger.debug("Admin {} récupère statistiques offres privées",
                getAuthenticatedUserEmail());

        Map<String, Long> stats = offrePriveeService.getStatistics();

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/admin/destinataire/{destinataireId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OffrePriveeResponse>> getOffresForDestinataire(
            @PathVariable Long destinataireId) {

        logger.debug("Admin {} liste offres privées destinataire id={}",
                getAuthenticatedUserEmail(), destinataireId);

        return ResponseEntity.ok(offrePriveeService.getOffresPriveesForDestinataire(destinataireId));
    }

    // ============================================
    // ENDPOINTS ÉTUDIANT (Consultation)
    // ============================================

    @GetMapping("/me")
    public ResponseEntity<List<OffrePriveeResponse>> getMyOffresPrivees() {

        Long etudiantId = getCurrentUserId(); // Utilise la méthode de BaseController

        logger.debug("Étudiant {} récupère ses offres privées", etudiantId);

        List<OffrePriveeResponse> offres = offrePriveeService
                .getMyOffresPrivees(etudiantId);

        return ResponseEntity.ok(offres);
    }

    @GetMapping("/me/page")
    public ResponseEntity<PagedResponse<OffrePriveeResponse>> getMyOffresPriveesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long etudiantId = getCurrentUserId();

        logger.debug("Étudiant {} récupère offres paginées: page={}, size={}",
                etudiantId, page, size);

        Page<OffrePriveeResponse> offresPage = offrePriveeService
                .getMyOffresPriveesPage(etudiantId, page, size);

        PagedResponse<OffrePriveeResponse> response = PagedResponse.of(offresPage);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/count-unread")
    public ResponseEntity<Map<String, Long>> countUnread() {

        Long etudiantId = getCurrentUserId();

        logger.debug("Étudiant {} compte offres non lues", etudiantId);

        Long count = offrePriveeService.countUnread(etudiantId);

        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/me/search")
    public ResponseEntity<List<OffrePriveeResponse>> searchMyOffres(
            @RequestParam String keyword) {

        Long etudiantId = getCurrentUserId();

        logger.debug("Étudiant {} recherche offres: keyword={}", etudiantId, keyword);

        List<OffrePriveeResponse> offres = offrePriveeService
                .searchMyOffres(etudiantId, keyword);

        return ResponseEntity.ok(offres);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OffrePriveeResponse> getOffrePrivee(@PathVariable Long id) {

        if (isCurrentUserAdmin()) {
            logger.debug("Admin {} récupère offre privée {}", getAuthenticatedUserEmail(), id);
            return ResponseEntity.ok(offrePriveeService.getOffrePriveeByIdForAdmin(id));
        }

        Long etudiantId = getCurrentUserId();

        logger.debug("Étudiant {} récupère offre {}", etudiantId, id);

        OffrePriveeResponse offre = offrePriveeService.getOffrePrivee(id, etudiantId);

        return ResponseEntity.ok(offre);
    }

    @PatchMapping("/{id}/mark-read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {

        Long etudiantId = getCurrentUserId();

        logger.debug("Étudiant {} marque offre {} comme lue", etudiantId, id);

        offrePriveeService.markAsVue(id, etudiantId);

        return ResponseEntity.ok().build();
    }
}