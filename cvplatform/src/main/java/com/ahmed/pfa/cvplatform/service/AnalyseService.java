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

    @Autowired
    private AnalyseIARepository analyseIARepository;

    @Autowired
    private RecommandationRepository recommandationRepository;

    @Autowired
    private CVRepository cvRepository;

    @Autowired
    private OffreEmploiRepository offreEmploiRepository;

    @Autowired
    private AIClientService aiClientService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Lancer une nouvelle analyse
     */
    @Transactional
    public AnalyseResponse lancerAnalyse(Long cvId, Long offreEmploiId) {
        logger.info("Lancement analyse: cvId={}, offreId={}", cvId, offreEmploiId);

        // 1. Récupérer CV et Offre
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("CV", cvId));

        OffreEmploi offre = offreEmploiRepository.findById(offreEmploiId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre d'emploi", offreEmploiId));

        // 2. Créer entité AnalyseIA (statut EN_COURS)
        AnalyseIA analyse = new AnalyseIA();
        analyse.setCv(cv);
        analyse.setOffreEmploi(offre);
        analyse.setStatut(AnalyseIA.StatutAnalyse.EN_COURS);
        analyse.setDateAnalyse(LocalDateTime.now());
        analyse.setScore(0.0);
        AnalyseIA savedAnalyse = analyseIARepository.save(analyse);

        try {
            // 3. Préparer les données (MOCK TEXT)
            String cvText = "Développeur Java avec 3 ans d'expérience en Spring Boot, " +
                    "MySQL, Git. Compétences: Java 17, Spring Framework, REST APIs, " +
                    "bases de données relationnelles. Formation en informatique.";

            String jobDescription = buildJobDescription(offre);

            // 4. Appeler l'IA (MOCK pour l'instant)
            AIAnalysisResult iaResult = aiClientService.analyzeCV(cvText, jobDescription);

            // 5. Sauvegarder les résultats
            savedAnalyse.setScore(iaResult.getScore());
            savedAnalyse.setCompetencesTrouvees(toJson(iaResult.getSkillsFound()));
            savedAnalyse.setCompetencesManquantes(toJson(iaResult.getMissingSkills()));
            savedAnalyse.setPointsForts(toJson(iaResult.getStrengths()));
            savedAnalyse.setPointsAmeliorer(toJson(iaResult.getImprovements()));
            savedAnalyse.setStatut(AnalyseIA.StatutAnalyse.TERMINEE);

            analyseIARepository.save(savedAnalyse);

            // 6. Sauvegarder les recommandations
            if (iaResult.getRecommendations() != null) {
                for (AIAnalysisResult.AIRecommendation iaReco : iaResult.getRecommendations()) {
                    Recommandation reco = new Recommandation();
                    reco.setAnalyseIA(savedAnalyse);
                    reco.setType(Recommandation.TypeRecommandation.valueOf(iaReco.getType()));
                    reco.setTexte(iaReco.getText());
                    reco.setPriorite(iaReco.getPriority());
                    reco.setCategorie(iaReco.getCategory());

                    recommandationRepository.save(reco);
                }
            }

            logger.info("Analyse terminée avec succès: analyseId={}, score={}",
                    savedAnalyse.getId(), savedAnalyse.getScore());

            // 7. Retourner le résultat
            return mapToResponse(savedAnalyse);

        } catch (Exception e) {
            logger.error("Erreur lors de l'analyse: {}", e.getMessage(), e);

            // Marquer l'analyse comme en erreur
            savedAnalyse.setStatut(AnalyseIA.StatutAnalyse.ERREUR);
            savedAnalyse.setMessageErreur(e.getMessage());
            analyseIARepository.save(savedAnalyse);

            throw new RuntimeException("Erreur lors de l'analyse IA: " + e.getMessage());
        }
    }

    /**
     * Récupérer une analyse par ID
     */
    @Transactional(readOnly = true)
    public AnalyseResponse getAnalyse(Long analyseId) {
        AnalyseIA analyse = analyseIARepository.findById(analyseId)
                .orElseThrow(() -> new ResourceNotFoundException("Analyse", analyseId));

        return mapToResponse(analyse);
    }

    /**
     * Récupérer toutes les analyses d'un CV
     */
    @Transactional(readOnly = true)
    public List<AnalyseResponse> getAnalysesByCv(Long cvId) {
        List<AnalyseIA> analyses = analyseIARepository.findByCvId(cvId);
        return analyses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Construire description complète de l'offre
     */
    private String buildJobDescription(OffreEmploi offre) {
        StringBuilder sb = new StringBuilder();
        sb.append("Poste: ").append(offre.getTitre()).append("\n");
        sb.append("Entreprise: ").append(offre.getEntreprise()).append("\n");
        if (offre.getDescription() != null) {
            sb.append("Description: ").append(offre.getDescription()).append("\n");
        }
        if (offre.getTypeContrat() != null) {
            sb.append("Type de contrat: ").append(offre.getTypeContrat()).append("\n");
        }
        if (offre.getCompetences() != null) {
            sb.append("Compétences requises: ").append(offre.getCompetences()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Convertir liste en JSON
     */
    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            logger.error("Erreur sérialisation JSON", e);
            return "[]";
        }
    }

    /**
     * Convertir JSON en liste
     */
    private List<String> fromJson(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            logger.error("Erreur désérialisation JSON", e);
            return List.of();
        }
    }

    /**
     * Mapper AnalyseIA vers AnalyseResponse
     */
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

        // Infos CV
        response.setCvId(analyse.getCv().getId());
        response.setCvNom(analyse.getCv().getNomFichier());

        // Infos Offre
        response.setOffreEmploiId(analyse.getOffreEmploi().getId());
        response.setOffreTitre(analyse.getOffreEmploi().getTitre());
        response.setOffreEntreprise(analyse.getOffreEmploi().getEntreprise());

        // Recommandations
        List<Recommandation> recommandations = recommandationRepository
                .findByAnalyseIAIdOrderByPrioriteAsc(analyse.getId());

        response.setRecommandations(
                recommandations.stream()
                        .map(RecommandationResponse::new)
                        .collect(Collectors.toList())
        );

        return response;
    }
}