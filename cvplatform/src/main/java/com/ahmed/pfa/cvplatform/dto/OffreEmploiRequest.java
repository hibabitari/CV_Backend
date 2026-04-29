package com.ahmed.pfa.cvplatform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OffreEmploiRequest {
    private String titre;
    private String entreprise;
    private String description;
    private String localisation;
    private String typeContrat;
    private String niveauExperience;
    private Double salaireMin;
    private Double salaireMax;
    private String competences; // Séparées par virgule: "Java,Spring,MySQL"
    private LocalDateTime dateExpiration;
    private Long etudiantId; // Optionnel
}