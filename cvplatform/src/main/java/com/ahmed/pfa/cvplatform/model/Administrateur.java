package com.ahmed.pfa.cvplatform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "administrateur")
@DiscriminatorValue("ADMIN")
public class Administrateur extends Utilisateur {

    @Column(name = "date_derniere_connexion")
    private LocalDateTime dateDerniereConnexion;

    @Column(name = "permissions")
    private String permissions; // Ex: "FULL_ACCESS", "READ_ONLY"
}