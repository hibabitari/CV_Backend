package com.ahmed.pfa.cvplatform.model;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "analyse_ia")
public class AnalyseIA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double score;

    @Column(name = "competences_trouvees", columnDefinition = "TEXT")
    private String competencesTrouvees;

    @Column(name = "competences_manquantes", columnDefinition = "TEXT")
    private String competencesManquantes;

    @Column(name = "points_forts", columnDefinition = "TEXT")
    private String pointsForts;

    @Column(name = "points_ameliorer", columnDefinition = "TEXT")
    private String pointsAmeliorer;

    @Column(name = "date_analyse", nullable = false)
    private LocalDateTime dateAnalyse;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatutAnalyse statut;

    @Column(name = "message_erreur", columnDefinition = "TEXT")
    private String messageErreur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    private CV cv;

    /**
     * Offre d'emploi de référence (pour offres publiques)
     * RENDU OPTIONNEL pour permettre les offres privées
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_emploi_id", nullable = true)
    private OffreEmploi offreEmploi;

    /**
     * Offre privée de référence (pour offres privées)
     * NOUVEAU CHAMP
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_privee_id", nullable = true)
    private OffrePrivee offrePrivee;

    public enum StatutAnalyse {
        EN_COURS,
        TERMINEE,
        ERREUR
    }
}