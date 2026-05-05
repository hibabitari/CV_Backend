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
    public static class AIRecommendation {
        private String type;
        private String text;
        private int priority;
        private String category;

        // AJOUTEZ CES DEUX CHAMPS ICI
        private String description;
        private String action;

        // Constructeur 4 params — celui utilisé dans AIClientService
        public AIRecommendation(String type, String text, int priority, String category, String description, String action) {
            this.type     = type;
            this.text     = text;
            this.priority = priority;
            this.category = category;
            this.description = description;
            this.action    = action;
        }

        // Générez les Getters et Setters pour 'description' et 'action'
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        // Gardez vos anciens getters/setters existants...
        public String getType() { return type; }
        public String getText() { return text; }
        public int getPriority() { return priority; }
        public String getCategory() { return category; }
    }
}