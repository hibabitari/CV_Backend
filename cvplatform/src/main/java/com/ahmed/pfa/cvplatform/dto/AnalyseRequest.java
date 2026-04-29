package com.ahmed.pfa.cvplatform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Requête pour lancer une analyse IA
 */
@Data
public class AnalyseRequest {

    @NotNull(message = "L'ID du CV est requis")
    private Long cvId;

    @NotNull(message = "L'ID de l'offre d'emploi est requis")
    private Long offreEmploiId;
}