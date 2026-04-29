package com.ahmed.pfa.cvplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour la réponse de l'API Python IA
 * Contrat d'interface avec le service d'analyse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIAnalysisResult {

    /**
     * Score de compatibilité (0-100)
     */
    private Double score;

    /**
     * Compétences trouvées
     */
    private List<String> skillsFound;

    /**
     * Compétences manquantes
     */
    private List<String> missingSkills;

    /**
     * Points forts
     */
    private List<String> strengths;

    /**
     * Points à améliorer
     */
    private List<String> improvements;

    /**
     * Recommandations générées
     */
    private List<AIRecommendation> recommendations;

    /**
     * Sous-classe pour les recommandations IA
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIRecommendation {
        private String type;
        private String text;
        private Integer priority;
        private String category;
    }
}