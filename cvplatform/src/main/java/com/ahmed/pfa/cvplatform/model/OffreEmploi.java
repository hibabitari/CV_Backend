package com.ahmed.pfa.cvplatform.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "offre_emploi")
public class OffreEmploi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String entreprise;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String localisation;

    @Column(name = "type_contrat")
    private String typeContrat; // "CDI", "CDD", "Stage", "Freelance"

    @Column(name = "niveau_experience")
    private String niveauExperience; // "Junior", "Confirmé", "Senior"

    @Column(name = "salaire_min")
    private Double salaireMin;

    @Column(name = "salaire_max")
    private Double salaireMax;

    @Column(columnDefinition = "TEXT")
    private String competences; // Liste de compétences séparées par virgule

    @Column(name = "date_publication", nullable = false)
    private LocalDateTime datePublication;

    @Column(name = "date_expiration")
    private LocalDateTime dateExpiration;

    private Boolean active = true; // Offre active ou expirée

    /** Null = offre publiée par un administrateur (visible par tous les étudiants). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id", nullable = true)
    private Etudiant etudiant;

    // Relations futures (Phase 5)
    // @OneToMany(mappedBy = "offreEmploi")
    // private List<AnalyseIA> analyses = new ArrayList<>();
}