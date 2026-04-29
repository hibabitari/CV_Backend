package com.ahmed.pfa.cvplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CVResponse {
    private Long id;
    private String nomFichier;
    private String typeFichier;
    private Long tailleFichier;
    private LocalDateTime dateUpload;
    private Long etudiantId;
    private String etudiantNom;
    private boolean analyseDisponible; // Pour Phase 5
}