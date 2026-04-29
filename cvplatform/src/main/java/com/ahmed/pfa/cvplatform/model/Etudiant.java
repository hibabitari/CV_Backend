package com.ahmed.pfa.cvplatform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "etudiant")
@DiscriminatorValue("ETUDIANT")
public class Etudiant extends Utilisateur {

    @Column(name = "niveau_etude", length = 255)
    private String niveauEtude;

    @Column(name = "domaine_etude", length = 255)
    private String domaineEtude;

    @Column(name = "universite", length = 255)
    private String universite;

    @OneToMany(mappedBy = "etudiant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CV> cvs = new ArrayList<>();

    // ✅ Maintenant on active cette relation
    @OneToMany(mappedBy = "etudiant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OffreEmploi> offresEmploi = new ArrayList<>();
}