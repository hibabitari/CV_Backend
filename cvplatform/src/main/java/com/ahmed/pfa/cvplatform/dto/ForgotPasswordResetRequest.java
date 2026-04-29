package com.ahmed.pfa.cvplatform.dto;

import com.ahmed.pfa.cvplatform.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordResetRequest {

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;

    @ValidPassword
    @NotBlank(message = "Le nouveau mot de passe est requis")
    private String nouveauMotDePasse;

    @NotBlank(message = "La confirmation du mot de passe est requise")
    private String confirmationMotDePasse;
}
