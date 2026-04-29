package com.ahmed.pfa.cvplatform.dto;

import com.ahmed.pfa.cvplatform.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour la requête de changement de mot de passe
 *
 * Sécurité:
 * - Ancien mot de passe requis (vérification identité)
 * - Nouveau mot de passe validé (force)
 * - Confirmation requise (éviter erreurs de saisie)
 *
 * @author Ahmed
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "L'ancien mot de passe est requis")
    private String ancienMotDePasse;

    @ValidPassword
    @NotBlank(message = "Le nouveau mot de passe est requis")
    private String nouveauMotDePasse;

    @NotBlank(message = "La confirmation du mot de passe est requise")
    private String confirmationMotDePasse;
}