package com.ahmed.pfa.cvplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OffreEmploiResponse {
    private Long id;
    private String titre;
    private String entreprise;
    private String description;
    private String localisation;
    private String typeContrat;
    private String niveauExperience;
    private Double salaireMin;
    private Double salaireMax;
    private String competences;
    private LocalDateTime datePublication;
    private LocalDateTime dateExpiration;
    private Boolean active;
    private Long etudiantId;
    private String etudiantNom;
}