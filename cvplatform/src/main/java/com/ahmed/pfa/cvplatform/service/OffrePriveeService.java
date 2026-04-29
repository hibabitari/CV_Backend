package com.ahmed.pfa.cvplatform.service;

import com.ahmed.pfa.cvplatform.dto.OffrePriveeRequest;
import com.ahmed.pfa.cvplatform.dto.OffrePriveeResponse;
import com.ahmed.pfa.cvplatform.exception.ResourceNotFoundException;
import com.ahmed.pfa.cvplatform.model.Etudiant;
import com.ahmed.pfa.cvplatform.model.OffrePrivee;
import com.ahmed.pfa.cvplatform.model.Utilisateur;
import com.ahmed.pfa.cvplatform.repository.EtudiantRepository;
import com.ahmed.pfa.cvplatform.repository.OffrePriveeRepository;
import com.ahmed.pfa.cvplatform.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des offres privées
 *
 * PRINCIPES:
 * - Toutes les opérations READ valident ownership
 * - Logging systématique des opérations sensibles
 * - @Transactional pour cohérence données
 * - Scheduled cleanup automatique
 *
 * @author Ahmed
 */
@Service
public class OffrePriveeService {

    private static final Logger logger = LoggerFactory.getLogger(OffrePriveeService.class);

    @Autowired
    private OffrePriveeRepository offrePriveeRepository;

    @Autowired
    private EtudiantRepository etudiantRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // ============================================
    // CRUD OPERATIONS (Admin)
    // ============================================

    /**
     * Créer une offre privée pour un étudiant
     *
     * SÉCURITÉ: Accessible seulement aux admins (@PreAuthorize dans Controller)
     *
     * @param request Données de l'offre
     * @param destinataireId ID de l'étudiant destinataire
     * @param emetteurEmail Email de l'admin émetteur
     * @return OffrePriveeResponse
     * @throws ResourceNotFoundException si destinataire n'existe pas
     */
    @Transactional
    public OffrePriveeResponse createOffrePrivee(
            OffrePriveeRequest request,
            Long destinataireId,
            String emetteurEmail) {

        logger.info("Création offre privée: destinataire={}, émetteur={}",
                destinataireId, emetteurEmail);

        // 1. Valider destinataire existe
        Etudiant destinataire = etudiantRepository.findById(destinataireId)
                .orElseThrow(() -> {
                    logger.error("Destinataire non trouvé: id={}", destinataireId);
                    return new ResourceNotFoundException("Étudiant", destinataireId);
                });

        // 2. Récupérer émetteur (optionnel, peut être null)
        Utilisateur emetteur = utilisateurRepository.findByEmail(emetteurEmail);
        if (emetteur == null) {
            logger.warn("Émetteur non trouvé: email={}", emetteurEmail);
        }

        // 3. Créer entity
        OffrePrivee offre = new OffrePrivee();
        offre.setTitre(request.getTitre());
        offre.setEntreprise(request.getEntreprise());
        offre.setDescription(request.getDescription());
        offre.setLocalisation(request.getLocalisation());
        offre.setTypeContrat(request.getTypeContrat());
        offre.setNiveauExperience(request.getNiveauExperience());
        offre.setSalaireMin(request.getSalaireMin());
        offre.setSalaireMax(request.getSalaireMax());
        offre.setCompetences(request.getCompetences());
        offre.setDateExpiration(request.getDateExpiration());

        // Relations
        offre.setDestinataire(destinataire);
        offre.setEmetteur(emetteur);

        // Statuts (defaults handled by entity)
        // active = true, vue = false set automatically

        // 4. Sauvegarder (dateCreation, dateEnvoi set by @PrePersist)
        OffrePrivee saved = offrePriveeRepository.save(offre);

        logger.info("Offre privée créée avec succès: offreId={}, destinataire={}",
                saved.getId(), destinataireId);

        // TODO Phase future: Send notification
        // notificationService.sendOffrePriveeNotification(saved);

        return mapToResponse(saved);
    }

    /**
     * Mettre à jour une offre privée
     *
     * @param offreId ID de l'offre
     * @param request Nouvelles données
     * @return OffrePriveeResponse
     * @throws ResourceNotFoundException si offre n'existe pas
     */
    @Transactional
    public OffrePriveeResponse updateOffrePrivee(Long offreId, OffrePriveeRequest request) {
        logger.info("Mise à jour offre privée: offreId={}", offreId);

        OffrePrivee offre = offrePriveeRepository.findById(offreId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre privée", offreId));

        // Update fields
        offre.setTitre(request.getTitre());
        offre.setEntreprise(request.getEntreprise());
        offre.setDescription(request.getDescription());
        offre.setLocalisation(request.getLocalisation());
        offre.setTypeContrat(request.getTypeContrat());
        offre.setNiveauExperience(request.getNiveauExperience());
        offre.setSalaireMin(request.getSalaireMin());
        offre.setSalaireMax(request.getSalaireMax());
        offre.setCompetences(request.getCompetences());
        offre.setDateExpiration(request.getDateExpiration());

        OffrePrivee updated = offrePriveeRepository.save(offre);

        logger.info("Offre privée mise à jour: offreId={}", offreId);

        return mapToResponse(updated);
    }

    /**
     * Désactiver une offre
     *
     * @param offreId ID de l'offre
     * @param raison Raison de désactivation
     * @throws ResourceNotFoundException si offre n'existe pas
     */
    @Transactional
    public void desactiverOffre(Long offreId, String raison) {
        logger.info("Désactivation offre privée: offreId={}, raison={}", offreId, raison);

        OffrePrivee offre = offrePriveeRepository.findById(offreId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre privée", offreId));

        offre.desactiver(raison);
        offrePriveeRepository.save(offre);

        logger.info("Offre privée désactivée: offreId={}", offreId);
    }

    /**
     * Supprimer une offre (hard delete)
     *
     * ATTENTION: Utiliser avec prudence, préférer désactivation
     *
     * @param offreId ID de l'offre
     * @throws ResourceNotFoundException si offre n'existe pas
     */
    @Transactional
    public void deleteOffre(Long offreId) {
        logger.warn("Suppression définitive offre privée: offreId={}", offreId);

        OffrePrivee offre = offrePriveeRepository.findById(offreId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre privée", offreId));

        offrePriveeRepository.delete(offre);

        logger.info("Offre privée supprimée: offreId={}", offreId);
    }

    // ============================================
    // QUERIES ÉTUDIANT (Ownership validated)
    // ============================================

    /**
     * Récupérer MES offres privées actives
     *
     * SÉCURITÉ: Filtré par destinataire_id (ownership)
     *
     * @param etudiantId ID de l'étudiant connecté
     * @return Liste des offres actives
     */
    @Transactional(readOnly = true)
    public List<OffrePriveeResponse> getMyOffresPrivees(Long etudiantId) {
        logger.debug("Récupération offres privées: etudiantId={}", etudiantId);

        List<OffrePrivee> offres = offrePriveeRepository
                .findByDestinataire_IdAndActiveTrue(etudiantId);

        logger.debug("Offres trouvées: {}", offres.size());

        return offres.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer MES offres avec pagination
     *
     * @param etudiantId ID de l'étudiant
     * @param page Numéro page
     * @param size Taille page
     * @return Page d'offres
     */
    @Transactional(readOnly = true)
    public Page<OffrePriveeResponse> getMyOffresPriveesPage(
            Long etudiantId, int page, int size) {

        logger.debug("Récupération offres privées paginées: etudiant={}, page={}, size={}",
                etudiantId, page, size);

        // Protection max size
        if (size > 100) size = 100;
        if (size < 1) size = 20;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("dateCreation").descending());

        Page<OffrePrivee> offresPage = offrePriveeRepository
                .findByDestinataire_IdAndActiveTrue(etudiantId, pageable);

        return offresPage.map(this::mapToResponse);
    }

    /**
     * Récupérer UNE offre par ID (avec ownership validation)
     *
     * SÉCURITÉ CRITIQUE:
     * - Valide que l'offre appartient à l'étudiant
     * - Si pas owner → 404 (pas 403, évite enumeration)
     *
     * @param offreId ID de l'offre
     * @param etudiantId ID de l'étudiant connecté
     * @return OffrePriveeResponse
     * @throws ResourceNotFoundException si pas trouvée OU pas owner
     */
    /**
     * Détail d'une offre pour l'admin (sans filtre destinataire).
     */
    @Transactional(readOnly = true)
    public OffrePriveeResponse getOffrePriveeByIdForAdmin(Long offreId) {
        OffrePrivee offre = offrePriveeRepository.findById(offreId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre privée", offreId));
        return mapToResponse(offre);
    }

    /**
     * Liste des offres privées d'un étudiant (admin).
     */
    @Transactional(readOnly = true)
    public List<OffrePriveeResponse> getOffresPriveesForDestinataire(Long destinataireId) {
        if (!etudiantRepository.existsById(destinataireId)) {
            throw new ResourceNotFoundException("Étudiant", destinataireId);
        }
        return offrePriveeRepository.findByDestinataire_IdOrderByDateCreationDesc(destinataireId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OffrePriveeResponse getOffrePrivee(Long offreId, Long etudiantId) {
        logger.debug("Récupération offre privée: offreId={}, etudiantId={}",
                offreId, etudiantId);

        // Query avec ownership check intégré
        OffrePrivee offre = offrePriveeRepository
                .findByIdAndDestinataire_Id(offreId, etudiantId)
                .orElseThrow(() -> {
                    // Ne pas révéler si offre existe mais pas owner
                    logger.warn("Offre non trouvée ou accès refusé: offreId={}, etudiantId={}",
                            offreId, etudiantId);
                    return new ResourceNotFoundException("Offre privée", offreId);
                });

        return mapToResponse(offre);
    }

    /**
     * Marquer une offre comme VUE
     *
     * SÉCURITÉ: Ownership validation
     *
     * @param offreId ID de l'offre
     * @param etudiantId ID de l'étudiant
     * @throws ResourceNotFoundException si pas trouvée
     * @throws AccessDeniedException si pas owner
     */
    @Transactional
    public void markAsVue(Long offreId, Long etudiantId) {
        logger.debug("Marquage comme vue: offreId={}, etudiantId={}", offreId, etudiantId);

        OffrePrivee offre = offrePriveeRepository.findById(offreId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre privée", offreId));

        // SECURITY CHECK
        if (!offre.appartientA(etudiantId)) {
            logger.warn("Tentative accès non autorisé: offreId={}, etudiantId={}",
                    offreId, etudiantId);
            throw new AccessDeniedException("Vous n'avez pas accès à cette offre");
        }

        // Idempotent: ne fait rien si déjà vue
        offre.marquerCommeLue();
        offrePriveeRepository.save(offre);

        logger.info("Offre marquée comme vue: offreId={}", offreId);
    }

    /**
     * Compter MES offres non vues
     *
     * @param etudiantId ID de l'étudiant
     * @return Nombre d'offres non vues
     */
    @Transactional(readOnly = true)
    public Long countUnread(Long etudiantId) {
        return offrePriveeRepository
                .countByDestinataire_IdAndActiveTrueAndVueFalse(etudiantId);
    }

    /**
     * Rechercher dans MES offres par mot-clé
     *
     * @param etudiantId ID de l'étudiant
     * @param keyword Mot-clé
     * @return Liste des offres matchant
     */
    @Transactional(readOnly = true)
    public List<OffrePriveeResponse> searchMyOffres(Long etudiantId, String keyword) {
        logger.debug("Recherche offres: etudiant={}, keyword={}", etudiantId, keyword);

        List<OffrePrivee> offres = offrePriveeRepository
                .searchByKeyword(etudiantId, keyword);

        return offres.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // ADMIN QUERIES
    // ============================================

    /**
     * Récupérer TOUTES les offres (admin only)
     *
     * @param page Numéro page
     * @param size Taille page
     * @return Page de toutes les offres
     */
    @Transactional(readOnly = true)
    public Page<OffrePriveeResponse> getAllOffresPrivees(int page, int size) {
        logger.debug("Récupération toutes offres privées: page={}, size={}", page, size);

        if (size > 100) size = 100;
        if (size < 1) size = 20;

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("dateCreation").descending());

        Page<OffrePrivee> offresPage = offrePriveeRepository.findAll(pageable);

        return offresPage.map(this::mapToResponse);
    }

    /**
     * Statistiques globales (admin)
     *
     * @return Map avec stats
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getStatistics() {
        return java.util.Map.of(
                "total", offrePriveeRepository.count(),
                "actives", offrePriveeRepository.countByActive(true),
                "inactives", offrePriveeRepository.countByActive(false),
                "nonVues", offrePriveeRepository.countByVue(false)
        );
    }

    // ============================================
    // SCHEDULED TASKS (Cleanup)
    // ============================================

    /**
     * Désactiver les offres expirées (quotidien à 2h)
     *
     * Cron: 0 0 2 * * * = Chaque jour à 02:00:00
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void dailyCleanupExpiredOffres() {
        logger.info("Démarrage cleanup quotidien offres expirées");

        List<OffrePrivee> expired = offrePriveeRepository
                .findByDateExpirationBeforeAndActiveTrue(LocalDateTime.now());

        if (expired.isEmpty()) {
            logger.info("Aucune offre expirée trouvée");
            return;
        }

        for (OffrePrivee offre : expired) {
            offre.desactiver("Expirée automatiquement");
            logger.debug("Offre désactivée: offreId={}, expiration={}",
                    offre.getId(), offre.getDateExpiration());
        }

        offrePriveeRepository.saveAll(expired);

        logger.info("Cleanup terminé: {} offres désactivées", expired.size());
    }

    /**
     * Purge des offres anciennes (mensuel, 1er du mois à 3h)
     *
     * Supprime définitivement les offres inactives de plus de 3 mois
     *
     * Cron: 0 0 3 1 * * = 1er de chaque mois à 03:00:00
     */
    @Scheduled(cron = "0 0 3 1 * *")
    @Transactional
    public void monthlyPurgeOldOffres() {
        logger.info("Démarrage purge mensuelle offres anciennes");

        LocalDateTime cutoff = LocalDateTime.now().minusMonths(3);

        List<OffrePrivee> toDelete = offrePriveeRepository
                .findByDateExpirationBeforeAndActiveFalse(cutoff);

        if (toDelete.isEmpty()) {
            logger.info("Aucune offre à purger");
            return;
        }

        logger.warn("Suppression définitive de {} offres anciennes", toDelete.size());

        offrePriveeRepository.deleteAll(toDelete);

        logger.info("Purge terminée: {} offres supprimées", toDelete.size());
    }

    // ============================================
    // MAPPING (Entity → DTO)
    // ============================================

    /**
     * Mapper OffrePrivee entity → OffrePriveeResponse DTO
     *
     * DENORMALIZATION:
     * - Charge infos destinataire (nom, email)
     * - Charge infos émetteur si présent
     * - Calcule 'expired' flag
     *
     * @param offre Entity
     * @return DTO
     */
    private OffrePriveeResponse mapToResponse(OffrePrivee offre) {
        OffrePriveeResponse response = new OffrePriveeResponse();

        // Basic fields
        response.setId(offre.getId());
        response.setTitre(offre.getTitre());
        response.setEntreprise(offre.getEntreprise());
        response.setDescription(offre.getDescription());
        response.setLocalisation(offre.getLocalisation());
        response.setTypeContrat(offre.getTypeContrat());
        response.setNiveauExperience(offre.getNiveauExperience());
        response.setSalaireMin(offre.getSalaireMin());
        response.setSalaireMax(offre.getSalaireMax());
        response.setCompetences(offre.getCompetences());

        // Dates
        response.setDateCreation(offre.getDateCreation());
        response.setDateEnvoi(offre.getDateEnvoi());
        response.setDateExpiration(offre.getDateExpiration());
        response.setDateLecture(offre.getDateLecture());

        // Statuts
        response.setActive(offre.getActive());
        response.setVue(offre.getVue());
        response.setExpired(offre.isExpired());  // Calculated
        response.setRaisonDesactivation(offre.getRaisonDesactivation());

        // Destinataire (denormalized)
        if (offre.getDestinataire() != null) {
            Etudiant dest = offre.getDestinataire();
            response.setDestinataireId(dest.getId());
            response.setDestinataireNom(dest.getNom());
            response.setDestinatairePrenom(dest.getPrenom());
            response.setDestinataireEmail(dest.getEmail());
        }

        // Émetteur (denormalized, optionnel)
        if (offre.getEmetteur() != null) {
            Utilisateur emetteur = offre.getEmetteur();
            response.setEmetteurId(emetteur.getId());
            response.setEmetteurNom(emetteur.getNom());
            response.setEmetteurEmail(emetteur.getEmail());
        }

        return response;
    }
}