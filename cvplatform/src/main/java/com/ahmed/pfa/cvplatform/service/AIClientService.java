package com.ahmed.pfa.cvplatform.service;

import com.ahmed.pfa.cvplatform.dto.AIAnalysisResult;
import com.ahmed.pfa.cvplatform.exception.IAServiceException;
import com.ahmed.pfa.cvplatform.exception.IAServiceTimeoutException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIClientService {

    private static final Logger logger = LoggerFactory.getLogger(AIClientService.class);

    @Autowired
    @Qualifier("iaServiceRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // URL du pipeline Python FastAPI — port 8000 par défaut
    // Configurez dans application.properties : ia.api.url=http://localhost:8000/analyze
    @Value("${ia.api.url:http://localhost:8000/analyze}")
    private String iaApiUrl;

    @Value("${ia.api.timeout.seconds:60}")
    private int timeoutSeconds;

    public AIAnalysisResult analyzeCV(String cvText, String jobDescription) {
        logger.info("Appel Python pipeline — cvText: {} chars, jobDescription: {} chars",
                cvText.length(), jobDescription.length());

        long startTime = System.currentTimeMillis();

        try {
            AIAnalysisResult result = callPythonPipeline(cvText, jobDescription);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Analyse terminée en {}ms — score: {}", duration, result.getScore());

            return result;

        } catch (ResourceAccessException ex) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Timeout Python API après {}ms", duration, ex);
            throw new IAServiceTimeoutException(
                    "L'analyse IA a dépassé le délai autorisé. Vérifiez que Python FastAPI tourne sur le port 8000.",
                    ex,
                    timeoutSeconds * 1000L
            );
        } catch (IAServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Erreur inattendue lors de l'analyse", ex);
            throw new IAServiceException(
                    "Erreur lors de l'analyse: " + ex.getMessage(), ex
            );
        }
    }

    /**
     * Appelle le pipeline Python et mappe la réponse vers AIAnalysisResult.
     *
     * Le pipeline Python retourne :
     * {
     *   "status": "success",
     *   "score": { "score_final": 72.5, "niveau": "Bon", "detail": {...} },
     *   "recommandations": {
     *     "manquants": [...],
     *     "bonus": [...],
     *     "resume": "...",
     *     "blocks": [...]
     *   },
     *   "cv":    { "sections": { "SKILL": "...", "EXPERIENCE": "..." } },
     *   "offre": { "sections": { "REQUIREMENT": "..." } }
     * }
     */
    private AIAnalysisResult callPythonPipeline(String cvText, String jobDescription) throws Exception {

        // ── 1. Construire le body pour Python ────────────────────────────
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("cv_text", cvText);
        body.put("job_description", jobDescription);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        logger.debug("POST {} — payload prêt", iaApiUrl);

        // ── 2. Appel HTTP ─────────────────────────────────────────────────
        ResponseEntity<String> response = restTemplate.exchange(
                iaApiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IAServiceException(
                    "Python API a retourné: " + response.getStatusCode(),
                    "IA_NON_SUCCESS_STATUS",
                    response.getStatusCode().value()
            );
        }

        // ── 3. Parser la réponse JSON du pipeline Python ──────────────────
        JsonNode root = objectMapper.readTree(response.getBody());

        if (!"success".equals(root.path("status").asText())) {
            String msg = root.path("message").asText("Erreur inconnue du pipeline");
            throw new IAServiceException("Pipeline Python en erreur: " + msg, "PIPELINE_ERROR");
        }

        return mapPythonResponseToResult(root);
    }

    /**
     * Mappe la réponse JSON de Python vers AIAnalysisResult.
     *
     * Gère les deux cas :
     *   - score = { score_final, niveau, detail }  (scoring.py)
     *   - recommandations = { manquants, bonus, resume, blocks } (recommendation.py)
     */
    private AIAnalysisResult mapPythonResponseToResult(JsonNode root) {
        AIAnalysisResult result = new AIAnalysisResult();

        // ── Score ─────────────────────────────────────────────────────────
        JsonNode scoreNode = root.path("score");
        double scoreFinal = scoreNode.path("score_final").asDouble(0.0);
        result.setScore(scoreFinal);

        // ── Compétences trouvées vs manquantes ────────────────────────────
        // Python ne calcule pas skillsFound directement — on le déduit :
        // skillsFound  = compétences dans le CV ET dans l'offre (via sections)
        // missingSkills = recommandations.manquants

        JsonNode recoNode = root.path("recommandations");

        // Compétences manquantes (depuis recommandation.py)
        result.setMissingSkills(jsonArrayToList(recoNode.path("manquants")));

        // Compétences trouvées = skills du CV qui matchent l'offre
        // Extraites depuis les sections cv.SKILL si disponible
        JsonNode cvSections = root.path("cv").path("sections");
        List<String> skillsTrouves = extractSkillsFromSection(cvSections.path("SKILL").asText(""));
        result.setSkillsFound(skillsTrouves);

        // ── Points forts ──────────────────────────────────────────────────
        // Construits depuis le score détaillé et le niveau
        String niveau = scoreNode.path("niveau").asText("Bon");
        JsonNode detail = scoreNode.path("detail");
        result.setStrengths(buildPointsForts(niveau, detail, cvSections));

        // ── Points à améliorer ────────────────────────────────────────────
        List<String> improvements = new ArrayList<>();
        String resume = recoNode.path("resume").asText("");
        if (!resume.isBlank()) improvements.add(resume);
        List<String> bonus = jsonArrayToList(recoNode.path("bonus"));
        bonus.stream().limit(2).forEach(b -> improvements.add("Renforcer : " + b));
        result.setImprovements(improvements);

        // ── Recommandations structurées ───────────────────────────────────
        List<AIAnalysisResult.AIRecommendation> recommendations = new ArrayList<>();
        JsonNode blocks = recoNode.path("blocks");
        if (blocks.isArray()) {
            for (JsonNode block : blocks) {
                String title = block.path("title").asText("");
                String value = block.path("value").asText("");

                // Blocs avec items (listes de compétences)
                if (block.has("items")) {
                    List<String> items = jsonArrayToList(block.path("items"));
                    if (!items.isEmpty()) {
                        String texte = String.join(", ", items);
                        recommendations.add(new AIAnalysisResult.AIRecommendation(
                                toEnumType(title), texte, prioriteFromTitle(title), categoryFromTitle(title), null, null
                        ));
                    }
                } else if (!value.isBlank()) {
                    recommendations.add(new AIAnalysisResult.AIRecommendation(
                            toEnumType(title), value, prioriteFromTitle(title), categoryFromTitle(title), null, null
                    ));
                }
            }
        }

        // Ajouter recommandations bonus/formation si blocks vides
        if (recommendations.isEmpty()) {
            List<String> manquants = result.getMissingSkills();
            if (!manquants.isEmpty()) {
                recommendations.add(new AIAnalysisResult.AIRecommendation(
                        "COMPETENCE_A_ACQUERIR",
                        "Acquérir les compétences manquantes : " + String.join(", ", manquants),
                        1, "FORMATION", null, null
                ));
            }
            if (!resume.isBlank()) {
                recommendations.add(new AIAnalysisResult.AIRecommendation(
                        "CONSEIL_ENTRETIEN", resume, 2, "ENTRETIEN", null, null
                ));
            }
        }

        result.setRecommendations(recommendations);

        logger.info("Mapping Python→Java terminé — score={}, skillsFound={}, manquants={}, reco={}",
                scoreFinal, skillsTrouves.size(), result.getMissingSkills().size(), recommendations.size());

        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ════════════════════════════════════════════════════════════════════════

    private List<String> jsonArrayToList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String val = item.asText("").trim();
                if (!val.isBlank()) list.add(val);
            }
        }
        return list;
    }

    /**
     * Extrait une liste de compétences depuis le texte brut de la section SKILL du CV.
     * Retourne les 8 premiers mots-clés significatifs.
     */
    private List<String> extractSkillsFromSection(String skillText) {
        List<String> skills = new ArrayList<>();
        if (skillText == null || skillText.isBlank()) return skills;

        String[] tokens = skillText.split("[,;\\n\\r/|]+");
        for (String token : tokens) {
            String t = token.trim();
            if (t.length() >= 2 && t.length() <= 30 && !t.isBlank()) {
                skills.add(capitalize(t));
                if (skills.size() >= 8) break;
            }
        }
        return skills;
    }

    private List<String> buildPointsForts(String niveau, JsonNode detail, JsonNode cvSections) {
        List<String> forts = new ArrayList<>();

        double skillScore = detail.path("SKILL").asDouble(0);
        double expScore   = detail.path("EXPERIENCE").asDouble(0);
        double eduScore   = detail.path("EDUCATION").asDouble(0);

        if (skillScore >= 50) forts.add("Compétences techniques bien alignées avec l'offre (" + Math.round(skillScore) + "%)");
        if (expScore   >= 50) forts.add("Expérience professionnelle pertinente (" + Math.round(expScore) + "%)");
        if (eduScore   >= 50) forts.add("Formation académique adaptée au poste (" + Math.round(eduScore) + "%)");

        if (forts.isEmpty()) {
            forts.add("Profil de niveau : " + niveau);
            String exp = cvSections.path("EXPERIENCE").asText("").trim();
            if (!exp.isBlank()) forts.add("Expérience mentionnée dans le CV");
        }

        return forts;
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String toEnumType(String title) {
        if (title == null) return "CONSEIL_ENTRETIEN";
        String t = title.toLowerCase();
        if (t.contains("manqu") || t.contains("ajouter")) return "COMPETENCE_A_ACQUERIR";
        if (t.contains("utile") || t.contains("renforc")) return "FORMATION_SUGGEREE";
        if (t.contains("domai")) return "AMELIORATION_CV";
        return "CONSEIL_ENTRETIEN";
    }

    private int prioriteFromTitle(String title) {
        if (title == null) return 3;
        String t = title.toLowerCase();
        if (t.contains("manqu") || t.contains("ajouter")) return 1;
        if (t.contains("utile") || t.contains("renforc")) return 2;
        return 3;
    }

    private String categoryFromTitle(String title) {
        if (title == null) return "ENTRETIEN";
        String t = title.toLowerCase();
        if (t.contains("manqu") || t.contains("ajouter")) return "FORMATION";
        if (t.contains("utile") || t.contains("renforc")) return "CLOUD";
        if (t.contains("domai")) return "CV";
        return "ENTRETIEN";
    }

    public boolean testIAApiConnection() {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    iaApiUrl.replace("/analyze", "/health"),
                    HttpMethod.GET, entity, String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            logger.error("Test connexion Python API échoué", ex);
            return false;
        }
    }
}