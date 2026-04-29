package com.ahmed.pfa.cvplatform.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Recommandation générée par l'IA
 */
@Data
@Entity
@Table(name = "recommandation")
public class Recommandation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type de recommandation
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TypeRecommandation type;

    /**
     * Texte de la recommandation
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String texte;

    /**
     * Priorité (1 = haute, 2 = moyenne, 3 = basse)
     */
    @Column(nullable = false)
    private Integer priorite;

    /**
     * Catégorie (COMPETENCE, EXPERIENCE, FORMATION, etc.)
     */
    @Column(length = 100)
    private String categorie;

    /**
     * Analyse IA associée
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyse_ia_id", nullable = false)
    private AnalyseIA analyseIA;

    /**
     * Types de recommandation
     */
    public enum TypeRecommandation {
        COMPETENCE_A_ACQUERIR,
        EXPERIENCE_A_VALORISER,
        FORMATION_SUGGEREE,
        AMELIORATION_CV,
        CONSEIL_ENTRETIEN
    }
}