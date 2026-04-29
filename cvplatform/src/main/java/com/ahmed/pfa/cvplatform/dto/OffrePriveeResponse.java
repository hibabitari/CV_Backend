package com.ahmed.pfa.cvplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour la réponse d'une offre privée
 *
 * USAGE: Retourné par tous les endpoints GET
 *
 * DENORMALIZATION:
 * - Inclut infos destinataire (nom, email) pour éviter N+1 queries
 * - Frontend a toutes les infos en 1 call
 *
 * @author Ahmed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OffrePriveeResponse {

    // Identifiant
    private Long id;

    // Informations offre
    private String titre;
    private String entreprise;
    private String description;
    private String localisation;
    private String typeContrat;
    private String niveauExperience;
    private Double salaireMin;
    private Double salaireMax;
    private String competences;

    // Dates
    private LocalDateTime dateCreation;
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateExpiration;
    private LocalDateTime dateLecture;

    // Statuts
    private Boolean active;
    private Boolean vue;
    private Boolean expired;  // Calculé

    // Informations destinataire (denormalized)
    private Long destinataireId;
    private String destinataireNom;
    private String destinatairePrenom;
    private String destinataireEmail;

    // Informations émetteur (optionnel)
    private Long emetteurId;
    private String emetteurNom;
    private String emetteurEmail;

    // Raison désactivation (si applicable)
    private String raisonDesactivation;
}