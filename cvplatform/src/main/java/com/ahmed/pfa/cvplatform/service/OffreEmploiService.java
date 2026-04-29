package com.ahmed.pfa.cvplatform.service;

import com.ahmed.pfa.cvplatform.dto.OffreEmploiRequest;
import com.ahmed.pfa.cvplatform.dto.OffreEmploiResponse;
import com.ahmed.pfa.cvplatform.exception.ResourceNotFoundException;
import com.ahmed.pfa.cvplatform.model.Etudiant;
import com.ahmed.pfa.cvplatform.model.OffreEmploi;
import com.ahmed.pfa.cvplatform.repository.AnalyseIARepository;
import com.ahmed.pfa.cvplatform.repository.EtudiantRepository;
import com.ahmed.pfa.cvplatform.repository.OffreEmploiRepository;
import com.ahmed.pfa.cvplatform.repository.RecommandationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OffreEmploiService {

    @Autowired
    private OffreEmploiRepository offreEmploiRepository;

    @Autowired
    private EtudiantRepository etudiantRepository;

    @Autowired
    private AnalyseIARepository analyseIARepository;

    @Autowired
    private RecommandationRepository recommandationRepository;

    /**
     * @param currentUserId ID JWT (étudiant ou admin)
     * @param isAdmin         si true : offre publique (etudiant_id = null) ; sinon : rattachée à l'étudiant connecté
     */
    @Transactional
    public OffreEmploiResponse createOffre(OffreEmploiRequest request, Long currentUserId, boolean isAdmin) {
        if (request.getTitre() == null || request.getTitre().isBlank()) {
            throw new IllegalArgumentException("Le titre est requis");
        }
        if (request.getEntreprise() == null || request.getEntreprise().isBlank()) {
            throw new IllegalArgumentException("L'entreprise est requise");
        }

        // Champs obligatoires côté étudiant (offre cible)
        if (!isAdmin) {
            if (request.getLocalisation() == null || request.getLocalisation().isBlank()) {
                throw new IllegalArgumentException("La localisation est requise");
            }
            if (request.getCompetences() == null || request.getCompetences().isBlank()) {
                throw new IllegalArgumentException("Les compétences sont requises");
            }
            if (request.getTypeContrat() == null || request.getTypeContrat().isBlank()) {
                throw new IllegalArgumentException("Le type de contrat est requis");
            }
            if (request.getDescription() == null || request.getDescription().isBlank()) {
                throw new IllegalArgumentException("La description est requise");
            }
        }

        OffreEmploi offre = new OffreEmploi();
        offre.setTitre(request.getTitre().trim());
        offre.setEntreprise(request.getEntreprise().trim());
        offre.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        offre.setLocalisation(request.getLocalisation() != null ? request.getLocalisation().trim() : null);
        offre.setTypeContrat(request.getTypeContrat() != null ? request.getTypeContrat().trim() : null);
        offre.setNiveauExperience(request.getNiveauExperience() != null ? request.getNiveauExperience().trim() : null);
        offre.setSalaireMin(request.getSalaireMin());
        offre.setSalaireMax(request.getSalaireMax());
        offre.setCompetences(request.getCompetences() != null ? request.getCompetences().trim() : null);
        offre.setDatePublication(LocalDateTime.now());
        offre.setDateExpiration(request.getDateExpiration());
        offre.setActive(true);

        // Si admin => offre publique (etudiant_id = null)
        // Sinon => offre rattachée à l'étudiant connecté
        if (isAdmin) {
            offre.setEtudiant(null);
        } else {
            Etudiant etudiant = etudiantRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Étudiant", currentUserId));
            offre.setEtudiant(etudiant);
        }

        OffreEmploi saved = offreEmploiRepository.save(offre);
        return mapToResponse(saved);
    }

    // ========== MÉTHODES SANS PAGINATION (anciennes - gardées) ==========

    @Transactional(readOnly = true)
    public List<OffreEmploiResponse> getAllOffresActives(Long currentUserId, boolean isAdmin) {
        List<OffreEmploi> offres;
        if (isAdmin) {
            offres = offreEmploiRepository.findByActiveTrue();
        } else {
            // Pour l'étudiant: voir offres admin (etudiant = null) + ses propres offres actives
            List<OffreEmploi> adminOffres = offreEmploiRepository.findByActiveTrueAndEtudiantIsNull();
            List<OffreEmploi> studentOffres = offreEmploiRepository.findByEtudiantIdAndActiveTrue(currentUserId);
            offres = java.util.stream.Stream.concat(adminOffres.stream(), studentOffres.stream())
                    .collect(Collectors.toList());
        }
        return offres.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OffreEmploiResponse> getAllOffres(Long currentUserId, boolean isAdmin) {
        List<OffreEmploi> offres;
        if (isAdmin) {
            offres = offreEmploiRepository.findAll();
        } else {
            // Étudiant: active seulement (admin + soi-même)
            List<OffreEmploi> adminOffres = offreEmploiRepository.findByActiveTrueAndEtudiantIsNull();
            List<OffreEmploi> studentOffres = offreEmploiRepository.findByEtudiantIdAndActiveTrue(currentUserId);
            offres = java.util.stream.Stream.concat(adminOffres.stream(), studentOffres.stream())
                    .collect(Collectors.toList());
        }
        return offres.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OffreEmploiResponse getOffreById(Long id, Long currentUserId, boolean isAdmin) {
        OffreEmploi offre = offreEmploiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre", id));
        if (!isAdmin) {
            // Étudiant ne peut voir que les offres actives
            if (!Boolean.TRUE.equals(offre.getActive())) {
                throw new AccessDeniedException("Accès refusé");
            }
            // Peut voir:
            // - offres admin (etudiant = null)
            // - ses propres offres (etudiant_id = currentUserId)
            if (offre.getEtudiant() != null && !offre.getEtudiant().getId().equals(currentUserId)) {
                throw new AccessDeniedException("Accès refusé");
            }
        }
        return mapToResponse(offre);
    }

    @Transactional(readOnly = true)
    public List<OffreEmploiResponse> getOffresByEtudiant(Long etudiantId) {
        List<OffreEmploi> offres = offreEmploiRepository.findByEtudiantId(etudiantId);
        return offres.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OffreEmploiResponse> searchOffres(String keyword, Long currentUserId, boolean isAdmin) {
        List<OffreEmploi> offres;
        if (isAdmin) {
            offres = offreEmploiRepository.searchGlobal(keyword);
        } else {
            // Étudiant: offres admin + ses offres à lui seulement (actives)
            List<OffreEmploi> adminOffres = offreEmploiRepository.searchAdmin(keyword).stream()
                    .filter(o -> Boolean.TRUE.equals(o.getActive()))
                    .collect(Collectors.toList());
            List<OffreEmploi> studentOffres = offreEmploiRepository.searchStudent(keyword, currentUserId).stream()
                    .filter(o -> Boolean.TRUE.equals(o.getActive()))
                    .collect(Collectors.toList());

            offres = java.util.stream.Stream.concat(adminOffres.stream(), studentOffres.stream())
                    .collect(Collectors.toList());
        }
        return offres.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OffreEmploiResponse> getOffresByLocalisation(String localisation, Long currentUserId, boolean isAdmin) {
        List<OffreEmploi> offres;
        if (isAdmin) {
            offres = offreEmploiRepository.findByLocalisation(localisation);
        } else {
            List<OffreEmploi> adminOffres = offreEmploiRepository.findByLocalisationAndEtudiantIsNull(localisation).stream()
                    .filter(o -> Boolean.TRUE.equals(o.getActive()))
                    .collect(Collectors.toList());
            List<OffreEmploi> studentOffres = offreEmploiRepository.findByLocalisationAndEtudiantId(localisation, currentUserId).stream()
                    .filter(o -> Boolean.TRUE.equals(o.getActive()))
                    .collect(Collectors.toList());
            offres = java.util.stream.Stream.concat(adminOffres.stream(), studentOffres.stream())
                    .collect(Collectors.toList());
        }
        return offres.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OffreEmploiResponse> getOffresByTypeContrat(String typeContrat, Long currentUserId, boolean isAdmin) {
        List<OffreEmploi> offres;
        if (isAdmin) {
            offres = offreEmploiRepository.findByTypeContrat(typeContrat);
        } else {
            List<OffreEmploi> adminOffres = offreEmploiRepository.findByTypeContratAndEtudiantIsNull(typeContrat).stream()
                    .filter(o -> Boolean.TRUE.equals(o.getActive()))
                    .collect(Collectors.toList());
            List<OffreEmploi> studentOffres = offreEmploiRepository.findByTypeContratAndEtudiantId(typeContrat, currentUserId).stream()
                    .filter(o -> Boolean.TRUE.equals(o.getActive()))
                    .collect(Collectors.toList());
            offres = java.util.stream.Stream.concat(adminOffres.stream(), studentOffres.stream())
                    .collect(Collectors.toList());
        }
        return offres.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========== NOUVELLES MÉTHODES AVEC PAGINATION ==========

    @Transactional(readOnly = true)
    public Page<OffreEmploiResponse> getAllOffresActivesPage(int page, int size, Long currentUserId, boolean isAdmin) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("datePublication").descending());
        Page<OffreEmploi> offresPage;
        if (isAdmin) {
            offresPage = offreEmploiRepository.findByActiveTrue(pageable);
        } else {
            List<OffreEmploi> combined = java.util.stream.Stream.concat(
                            offreEmploiRepository.findByActiveTrueAndEtudiantIsNull().stream(),
                            offreEmploiRepository.findByEtudiantIdAndActiveTrue(currentUserId).stream()
                    )
                    .sorted((a, b) -> b.getDatePublication().compareTo(a.getDatePublication()))
                    .collect(Collectors.toList());

            List<OffreEmploi> content = combined.stream()
                    .skip((long) page * size)
                    .limit(size)
                    .collect(Collectors.toList());

            offresPage = new org.springframework.data.domain.PageImpl<>(content, pageable, combined.size());
        }
        return offresPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<OffreEmploiResponse> getAllOffresPage(int page, int size, Long currentUserId, boolean isAdmin) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("datePublication").descending());
        Page<OffreEmploi> offresPage;
        if (isAdmin) {
            offresPage = offreEmploiRepository.findAll(pageable);
        } else {
            // Étudiant: active seulement (admin + soi-même)
            List<OffreEmploi> combined = java.util.stream.Stream.concat(
                            offreEmploiRepository.findByActiveTrueAndEtudiantIsNull().stream(),
                            offreEmploiRepository.findByEtudiantIdAndActiveTrue(currentUserId).stream()
                    )
                    .sorted((a, b) -> b.getDatePublication().compareTo(a.getDatePublication()))
                    .collect(Collectors.toList());

            List<OffreEmploi> content = combined.stream()
                    .skip((long) page * size)
                    .limit(size)
                    .collect(Collectors.toList());

            offresPage = new org.springframework.data.domain.PageImpl<>(content, pageable, combined.size());
        }
        return offresPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<OffreEmploiResponse> getOffresByLocalisationPage(String localisation, int page, int size, Long currentUserId, boolean isAdmin) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("datePublication").descending());
        Page<OffreEmploi> offresPage;
        if (isAdmin) {
            offresPage = offreEmploiRepository.findByLocalisation(localisation, pageable);
        } else {
            List<OffreEmploi> combined = java.util.stream.Stream.concat(
                            offreEmploiRepository.findByLocalisationAndEtudiantIsNull(localisation).stream(),
                            offreEmploiRepository.findByLocalisationAndEtudiantId(localisation, currentUserId).stream()
                    )
                    .filter(o -> Boolean.TRUE.equals(o.getActive()))
                    .sorted((a, b) -> b.getDatePublication().compareTo(a.getDatePublication()))
                    .collect(Collectors.toList());

            List<OffreEmploi> content = combined.stream()
                    .skip((long) page * size)
                    .limit(size)
                    .collect(Collectors.toList());

            offresPage = new org.springframework.data.domain.PageImpl<>(content, pageable, combined.size());
        }
        return offresPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<OffreEmploiResponse> getOffresByTypeContratPage(String typeContrat, int page, int size, Long currentUserId, boolean isAdmin) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("datePublication").descending());
        Page<OffreEmploi> offresPage;
        if (isAdmin) {
            offresPage = offreEmploiRepository.findByTypeContrat(typeContrat, pageable);
        } else {
            List<OffreEmploi> combined = java.util.stream.Stream.concat(
                            offreEmploiRepository.findByTypeContratAndEtudiantIsNull(typeContrat).stream(),
                            offreEmploiRepository.findByTypeContratAndEtudiantId(typeContrat, currentUserId).stream()
                    )
                    .filter(o -> Boolean.TRUE.equals(o.getActive()))
                    .sorted((a, b) -> b.getDatePublication().compareTo(a.getDatePublication()))
                    .collect(Collectors.toList());

            List<OffreEmploi> content = combined.stream()
                    .skip((long) page * size)
                    .limit(size)
                    .collect(Collectors.toList());

            offresPage = new org.springframework.data.domain.PageImpl<>(content, pageable, combined.size());
        }
        return offresPage.map(this::mapToResponse);
    }

    // ========== CRUD OPERATIONS ==========

    @Transactional
    public OffreEmploiResponse updateOffre(Long id, OffreEmploiRequest request, Long currentUserId, boolean isAdmin) {
        if (!isAdmin) {
            throw new AccessDeniedException("Seul un administrateur peut modifier une offre");
        }
        OffreEmploi offre = offreEmploiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre", id));

        if (request.getTitre() != null) offre.setTitre(request.getTitre());
        if (request.getEntreprise() != null) offre.setEntreprise(request.getEntreprise());
        if (request.getDescription() != null) offre.setDescription(request.getDescription());
        if (request.getLocalisation() != null) offre.setLocalisation(request.getLocalisation());
        if (request.getTypeContrat() != null) offre.setTypeContrat(request.getTypeContrat());
        if (request.getNiveauExperience() != null) offre.setNiveauExperience(request.getNiveauExperience());
        if (request.getSalaireMin() != null) offre.setSalaireMin(request.getSalaireMin());
        if (request.getSalaireMax() != null) offre.setSalaireMax(request.getSalaireMax());
        if (request.getCompetences() != null) offre.setCompetences(request.getCompetences());
        if (request.getDateExpiration() != null) offre.setDateExpiration(request.getDateExpiration());

        OffreEmploi updated = offreEmploiRepository.save(offre);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteOffre(Long id, Long currentUserId, boolean isAdmin) {
        if (!isAdmin) {
            throw new AccessDeniedException("Seul un administrateur peut supprimer une offre");
        }
        OffreEmploi offre = offreEmploiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre", id));
        // FK analyse_ia.offre_emploi_id : supprimer recommandations puis analyses (JPQL bulk, fiable sous MySQL/Hibernate)
        recommandationRepository.deleteAllForOffreEmploiId(id);
        analyseIARepository.deleteAllForOffreEmploiId(id);
        offreEmploiRepository.delete(offre);
    }

    @Transactional
    public OffreEmploiResponse desactiverOffre(Long id, Long currentUserId, boolean isAdmin) {
        if (!isAdmin) {
            throw new AccessDeniedException("Seul un administrateur peut désactiver une offre");
        }
        OffreEmploi offre = offreEmploiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre", id));
        offre.setActive(false);
        OffreEmploi updated = offreEmploiRepository.save(offre);
        return mapToResponse(updated);
    }

    private OffreEmploiResponse mapToResponse(OffreEmploi offre) {
        OffreEmploiResponse response = new OffreEmploiResponse();
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
        response.setDatePublication(offre.getDatePublication());
        response.setDateExpiration(offre.getDateExpiration());
        response.setActive(offre.getActive());

        if (offre.getEtudiant() != null) {
            response.setEtudiantId(offre.getEtudiant().getId());
            response.setEtudiantNom(offre.getEtudiant().getNom() + " " + offre.getEtudiant().getPrenom());
        }

        return response;
    }
}