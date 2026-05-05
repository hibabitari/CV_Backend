package com.ahmed.pfa.cvplatform.service;

import com.ahmed.pfa.cvplatform.dto.AIAnalysisResult;
import com.ahmed.pfa.cvplatform.dto.AnalyseResponse;
import com.ahmed.pfa.cvplatform.dto.RecommandationResponse;
import com.ahmed.pfa.cvplatform.exception.ResourceNotFoundException;
import com.ahmed.pfa.cvplatform.model.AnalyseIA;
import com.ahmed.pfa.cvplatform.model.CV;
import com.ahmed.pfa.cvplatform.model.OffreEmploi;
import com.ahmed.pfa.cvplatform.model.Recommandation;
import com.ahmed.pfa.cvplatform.repository.AnalyseIARepository;
import com.ahmed.pfa.cvplatform.repository.CVRepository;
import com.ahmed.pfa.cvplatform.repository.OffreEmploiRepository;
import com.ahmed.pfa.cvplatform.repository.RecommandationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyseService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyseService.class);

    @Autowired private AnalyseIARepository analyseIARepository;
    @Autowired private RecommandationRepository recommandationRepository;
    @Autowired private CVRepository cvRepository;
    @Autowired private OffreEmploiRepository offreEmploiRepository;
    @Autowired private AIClientService aiClientService;
    @Autowired private ObjectMapper objectMapper;

    // ── Services déjà existants dans ton projet ───────────────────────────
    @Autowired private FileStorageService fileStorageService;
    @Autowired private CvTextExtractionService cvTextExtractionService;

    @Transactional
    public AnalyseResponse lancerAnalyse(Long cvId, Long offreEmploiId) {
        logger.info("Lancement analyse: cvId={}, offreId={}", cvId, offreEmploiId);

        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("CV", cvId));

        OffreEmploi offre = offreEmploiRepository.findById(offreEmploiId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre d'emploi", offreEmploiId));

        AnalyseIA analyse = new AnalyseIA();
        analyse.setCv(cv);
        analyse.setOffreEmploi(offre);
        analyse.setStatut(AnalyseIA.StatutAnalyse.EN_COURS);
        analyse.setDateAnalyse(LocalDateTime.now());
        analyse.setScore(0.0);
        AnalyseIA savedAnalyse = analyseIARepository.save(analyse);

        try {
            // Extraction du texte
            String cvText = extraireTexteCv(cv);

            if (cvText == null || cvText.isBlank()) {
                throw new RuntimeException(
                        "Impossible d'extraire le texte du CV '" + cv.getNomFichier() + "'."
                );
            }

            logger.info("Texte extrait: {} caractères pour '{}'", cvText.length(), cv.getNomFichier());

            String jobDescription = buildJobDescription(offre);
            AIAnalysisResult iaResult = aiClientService.analyzeCV(cvText, jobDescription);

            savedAnalyse.setScore(iaResult.getScore());
            savedAnalyse.setCompetencesTrouvees(toJson(iaResult.getSkillsFound()));
            savedAnalyse.setCompetencesManquantes(toJson(iaResult.getMissingSkills()));
            savedAnalyse.setPointsForts(toJson(iaResult.getStrengths()));
            savedAnalyse.setPointsAmeliorer(toJson(iaResult.getImprovements()));

            // MISE À JOUR : On s'assure que le statut passe bien à TERMINEE
            savedAnalyse.setStatut(AnalyseIA.StatutAnalyse.TERMINEE); // Doit être exactement ce statut
            analyseIARepository.save(savedAnalyse); // Sauvegarde finale de l'analyse

            // ─── PARTIE MISE À JOUR POUR LES RECOMMANDATIONS PYTHON ───
            if (iaResult.getRecommendations() != null) {
                for (AIAnalysisResult.AIRecommendation iaReco : iaResult.getRecommendations()) {
                    Recommandation reco = new Recommandation();
                    reco.setAnalyseIA(savedAnalyse);

                    // Type de recommandation
                    try {
                        reco.setType(Recommandation.TypeRecommandation.valueOf(iaReco.getType().toUpperCase()));
                    } catch (Exception e) {
                        reco.setType(Recommandation.TypeRecommandation.AUTRE);
                    }

                    // 1. Gestion de la Priorité
                    reco.setPriorite(iaReco.getPriority());

                    // 2. Gestion de la Catégorie
                    reco.setCategorie(iaReco.getCategory() != null ? iaReco.getCategory() : iaReco.getType());

                    // 3. Gestion du Texte (Priorité à la description de l'IA Python)[cite: 2]
                    String contenu = (iaReco.getDescription() != null) ? iaReco.getDescription() : iaReco.getText();
                    reco.setTexte(contenu);

                    // 4. Gestion de l'Action (Nécessite le champ 'action' dans Recommandation.java)[cite: 2]
                    if (iaReco.getAction() != null) {
                        reco.setAction(iaReco.getAction());
                    }

                    recommandationRepository.save(reco);
                }
            }
            // ──────────────────────────────────────────────────────────

            logger.info("Analyse terminée: analyseId={}, score={}", savedAnalyse.getId(), savedAnalyse.getScore());
            return mapToResponse(savedAnalyse);

        } catch (Exception e) {
            logger.error("Erreur lors de l'analyse: {}", e.getMessage(), e);
            savedAnalyse.setStatut(AnalyseIA.StatutAnalyse.ERREUR);
            savedAnalyse.setMessageErreur(e.getMessage());
            analyseIARepository.save(savedAnalyse);
            throw new RuntimeException("Erreur lors de l'analyse IA: " + e.getMessage());
        }
    }

    private String extraireTexteCv(CV cv) throws Exception {
        if (cv.getContenuTexte() != null && !cv.getContenuTexte().isBlank()) {
            logger.info("✅ Texte récupéré depuis BDD pour '{}'", cv.getNomFichier());
            return cv.getContenuTexte();
        }

        logger.info("Relecture fichier via FileStorageService: '{}'", cv.getCheminFichier());
        byte[] bytes = fileStorageService.loadFileBytes(cv.getCheminFichier());

        String texte = cvTextExtractionService.extractPlainText(
                bytes,
                cv.getNomFichier(),
                cv.getTypeFichier()
        );

        if (texte != null && !texte.isBlank()) {
            cv.setContenuTexte(texte);
            cvRepository.save(cv);
            logger.info("Texte sauvegardé en BDD pour les prochaines analyses");
        }

        return texte;
    }

    @Transactional(readOnly = true)
    public AnalyseResponse getAnalyse(Long analyseId) {
        AnalyseIA analyse = analyseIARepository.findById(analyseId)
                .orElseThrow(() -> new ResourceNotFoundException("Analyse", analyseId));
        return mapToResponse(analyse);
    }

    @Transactional(readOnly = true)
    public List<AnalyseResponse> getAnalysesByCv(Long cvId) {
        return analyseIARepository.findByCvId(cvId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String buildJobDescription(OffreEmploi offre) {
        StringBuilder sb = new StringBuilder();
        sb.append("Poste: ").append(offre.getTitre()).append("\n");
        sb.append("Entreprise: ").append(offre.getEntreprise()).append("\n");
        if (offre.getDescription() != null)
            sb.append("Description: ").append(offre.getDescription()).append("\n");
        if (offre.getTypeContrat() != null)
            sb.append("Type de contrat: ").append(offre.getTypeContrat()).append("\n");
        if (offre.getCompetences() != null)
            sb.append("Compétences requises: ").append(offre.getCompetences()).append("\n");
        return sb.toString();
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            logger.error("Erreur sérialisation JSON", e);
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private AnalyseResponse mapToResponse(AnalyseIA analyse) {
        AnalyseResponse response = new AnalyseResponse();
        response.setId(analyse.getId());
        response.setScore(analyse.getScore());
        response.setCompetencesTrouvees(fromJson(analyse.getCompetencesTrouvees()));
        response.setCompetencesManquantes(fromJson(analyse.getCompetencesManquantes()));
        response.setPointsForts(fromJson(analyse.getPointsForts()));
        response.setPointsAmeliorer(fromJson(analyse.getPointsAmeliorer()));
        response.setDateAnalyse(analyse.getDateAnalyse());
        response.setStatut(analyse.getStatut().name());
        response.setMessageErreur(analyse.getMessageErreur());
        response.setCvId(analyse.getCv().getId());
        response.setCvNom(analyse.getCv().getNomFichier());
        response.setOffreEmploiId(analyse.getOffreEmploi().getId());
        response.setOffreTitre(analyse.getOffreEmploi().getTitre());
        response.setOffreEntreprise(analyse.getOffreEmploi().getEntreprise());

        List<Recommandation> recommandations = recommandationRepository
                .findByAnalyseIAIdOrderByPrioriteAsc(analyse.getId());
        response.setRecommandations(
                recommandations.stream().map(RecommandationResponse::new).collect(Collectors.toList())
        );
        return response;
    }
}