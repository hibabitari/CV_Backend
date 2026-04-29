package com.ahmed.pfa.cvplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour le résultat d'une analyse IA
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseResponse {

    private Long id;
    private Double score;
    private List<String> competencesTrouvees;
    private List<String> competencesManquantes;
    private List<String> pointsForts;
    private List<String> pointsAmeliorer;
    private LocalDateTime dateAnalyse;
    private String statut;
    private String messageErreur;

    // Informations CV
    private Long cvId;
    private String cvNom;

    // Informations Offre
    private Long offreEmploiId;
    private String offreTitre;
    private String offreEntreprise;

    // Recommandations
    private List<RecommandationResponse> recommandations;
}