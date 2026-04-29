package com.ahmed.pfa.cvplatform.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Analyse IA d'un CV par rapport à une offre d'emploi
 */
@Data
@Entity
@Table(name = "analyse_ia")
public class AnalyseIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Score de compatibilité (0-100)
     */
    @Column(nullable = false)
    private Double score;

    /**
     * Compétences trouvées dans le CV (JSON array)
     */
    @Column(name = "competences_trouvees", columnDefinition = "TEXT")
    private String competencesTrouvees;

    /**
     * Compétences manquantes (JSON array)
     */
    @Column(name = "competences_manquantes", columnDefinition = "TEXT")
    private String competencesManquantes;

    /**
     * Points forts identifiés (JSON array)
     */
    @Column(name = "points_forts", columnDefinition = "TEXT")
    private String pointsForts;

    /**
     * Points à améliorer (JSON array)
     */
    @Column(name = "points_ameliorer", columnDefinition = "TEXT")
    private String pointsAmeliorer;

    /**
     * Date de l'analyse
     */
    @Column(name = "date_analyse", nullable = false)
    private LocalDateTime dateAnalyse;

    /**
     * Statut de l'analyse
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatutAnalyse statut;

    /**
     * Message d'erreur si échec
     */
    @Column(name = "message_erreur", columnDefinition = "TEXT")
    private String messageErreur;

    /**
     * CV analysé
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    private CV cv;

    /**
     * Offre d'emploi de référence
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_emploi_id", nullable = false)
    private OffreEmploi offreEmploi;

    /**
     * Enum pour le statut de l'analyse
     */
    public enum StatutAnalyse {
        EN_COURS,
        TERMINEE,
        ERREUR
    }
}