package com.ahmed.pfa.cvplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité de base pour tous les utilisateurs du système
 *
 * Utilise l'héritage JOINED:
 * - Table utilisateur (commune)
 * - Table etudiant (spécifique)
 * - Table administrateur (spécifique)
 *
 * MODIFICATIONS SECURITY:
 * - @JsonIgnore sur motDePasse (empêche exposition du hash)
 * - typeUtilisateur ajouté (complément à role pour compatibilité)
 * - dateCreation ajoutée (audit trail)
 *
 * @author Ahmed
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "utilisateur")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 50)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 50)
    private String prenom;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    /**
     * Mot de passe hashé (BCrypt)
     * @JsonIgnore empêche l'exposition dans les responses API
     */
    @JsonIgnore
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    /**
     * Role utilisé pour Spring Security
     * Valeurs: "ADMIN", "ETUDIANT"
     */
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    /**
     * Type utilisateur (legacy, compatibilité)
     * Valeurs: "ADMIN", "ETUDIANT"
     */
    @Column(name = "type_utilisateur", nullable = false, length = 50)
    private String typeUtilisateur;

    /**
     * Date de création du compte
     * Auto-remplie lors de l'inscription
     */
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;


}