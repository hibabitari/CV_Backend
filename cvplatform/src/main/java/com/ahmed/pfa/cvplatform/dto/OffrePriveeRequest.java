package com.ahmed.pfa.cvplatform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO pour la création d'une offre privée
 *
 * USAGE: Admin crée offre pour un étudiant
 * POST /api/offres-privees?destinataireId=5
 *
 * VALIDATION:
 * - @Valid dans Controller déclenche validation
 * - MethodArgumentNotValidException si erreur
 * - GlobalExceptionHandler retourne 400 + details
 *
 * @author Ahmed
 */
@Data
public class OffrePriveeRequest {

    @NotBlank(message = "Le titre est requis")
    @Size(min = 5, max = 200, message = "Le titre doit contenir entre 5 et 200 caractères")
    private String titre;

    @NotBlank(message = "Le nom de l'entreprise est requis")
    @Size(max = 150, message = "Le nom de l'entreprise ne peut dépasser 150 caractères")
    private String entreprise;

    @NotBlank(message = "La description est requise")
    @Size(min = 20, max = 5000, message = "La description doit contenir entre 20 et 5000 caractères")
    private String description;

    @NotBlank(message = "La localisation est requise")
    @Size(max = 100, message = "La localisation ne peut dépasser 100 caractères")
    private String localisation;

    @NotBlank(message = "Le type de contrat est requis")
    @Pattern(
            regexp = "CDI|CDD|Stage|Freelance|Alternance",
            message = "Type de contrat invalide. Valeurs acceptées: CDI, CDD, Stage, Freelance, Alternance"
    )
    private String typeContrat;

    @Size(max = 50, message = "Le niveau d'expérience ne peut dépasser 50 caractères")
    private String niveauExperience;

    @Min(value = 0, message = "Le salaire minimum ne peut être négatif")
    private Double salaireMin;

    @Min(value = 0, message = "Le salaire maximum ne peut être négatif")
    private Double salaireMax;

    @Size(max = 1000, message = "Les compétences ne peuvent dépasser 1000 caractères")
    private String competences;

    @NotNull(message = "La date d'expiration est requise")
    @Future(message = "La date d'expiration doit être dans le futur")
    private LocalDateTime dateExpiration;
}