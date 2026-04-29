package com.ahmed.pfa.cvplatform.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO pour la mise à jour du profil
 */
@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String nom;

    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String prenom;

    @Size(max = 100, message = "Le niveau d'étude ne peut pas dépasser 100 caractères")
    private String niveauEtude;

    @Size(max = 100, message = "Le domaine d'étude ne peut pas dépasser 100 caractères")
    private String domaineEtude;

    @Size(max = 100, message = "L'université ne peut pas dépasser 100 caractères")
    private String universite;

    @Size(max = 255, message = "Les permissions ne peuvent pas dépasser 255 caractères")
    private String permissions;
}