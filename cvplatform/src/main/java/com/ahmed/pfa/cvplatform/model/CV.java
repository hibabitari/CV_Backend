package com.ahmed.pfa.cvplatform.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cv")
public class CV {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "chemin_fichier", nullable = false)
    private String cheminFichier;

    @Column(name = "type_fichier")
    private String typeFichier; // "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    @Column(name = "taille_fichier")
    private Long tailleFichier; // En bytes

    @Column(name = "contenu_texte", columnDefinition = "TEXT")
    private String contenuTexte; // Texte extrait du CV

    @Column(name = "date_upload", nullable = false)
    private LocalDateTime dateUpload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    // Relations futures (Phase 5)
    // @OneToMany(mappedBy = "cv", cascade = CascadeType.ALL)
    // private List<AnalyseIA> analyses = new ArrayList<>();
}