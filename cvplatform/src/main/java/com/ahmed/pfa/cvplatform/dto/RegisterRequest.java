package com.ahmed.pfa.cvplatform.dto;

import com.ahmed.pfa.cvplatform.validation.ValidPassword;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO pour la requête d'inscription
 *
 * MODIFICATIONS SECURITY PHASE 1:
 * - Remplacement @Pattern par @ValidPassword (validation forte)
 * - Critères: 8+ chars, majuscule, minuscule, chiffre, caractère spécial
 * - Séparation nom/prenom pour meilleure UX frontend
 *
 * @author Ahmed
 */
@Data
public class RegisterRequest {

    // ✅ NOM et PRENOM séparés (au lieu de nomComplet)
    @NotBlank(message = "Le nom est requis")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est requis")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String prenom;

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;

    @ValidPassword
    @NotBlank(message = "Le mot de passe est requis")
    private String motDePasse;

    /*@NotBlank(message = "La confirmation du mot de passe est requise")
    private String confirmMotDePasse;*/

    @NotBlank(message = "Le niveau d'étude est requis")
    private String niveauEtude;

    @NotBlank(message = "Le domaine d'étude est requis")
    private String domaineEtude;

    @NotBlank(message = "L'université est requise")
    private String universite;

    @NotBlank(message = "Le rôle est requis")
    @Pattern(regexp = "ETUDIANT|ADMIN", message = "Le rôle doit être ETUDIANT ou ADMIN")
    private String role;

    // Optionnel : Acceptation des conditions
    private Boolean acceptTerms;


    // ✅ Méthode utilitaire pour obtenir le nom complet (si besoin pour l'affichage)
    public String getNomComplet() {
        if (nom != null && prenom != null) {
            return prenom + " " + nom;
        }
        return nom != null ? nom : prenom;
    }


    // ✅ Méthode utilitaire pour la validation côté service
    /*public boolean isPasswordMatching() {
        return motDePasse != null && motDePasse.equals(confirmMotDePasse);
    }*/
}