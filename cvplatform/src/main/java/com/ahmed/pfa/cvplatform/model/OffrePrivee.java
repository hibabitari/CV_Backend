package com.ahmed.pfa.cvplatform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity représentant une offre d'emploi PRIVÉE destinée à un étudiant spécifique.
 *
 * DIFFÉRENCE vs OffreEmploi (publique):
 * - Visible uniquement par le destinataire (Etudiant)
 * - Envoyée directement par un Admin
 * - Cycle de vie géré (expiration, désactivation)
 *
 * ISOLATION:
 * - Aucune relation avec OffreEmploi (tables séparées)
 * - Queries toujours filtrées par destinataire_id
 * - Garantit privacy by design
 *
 * @author Ahmed
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "offre_privee",
        indexes = {
                // Index pour requêtes fréquentes (performance)
                @Index(name = "idx_destinataire_active", columnList = "destinataire_id, active"),
                @Index(name = "idx_date_expiration", columnList = "date_expiration"),
                @Index(name = "idx_active_vue", columnList = "active, vue")
        }
)
public class OffrePrivee {

    // ============================================
    // PRIMARY KEY
    // ============================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================
    // INFORMATIONS OFFRE (Similaire à OffreEmploi)
    // ============================================

    /**
     * Titre du poste
     * Exemple: "Développeur Java Backend - Mission Confidentielle"
     */
    @NotBlank(message = "Le titre est requis")
    @Size(min = 5, max = 200, message = "Le titre doit contenir entre 5 et 200 caractères")
    @Column(nullable = false, length = 200)
    private String titre;

    /**
     * Nom de l'entreprise
     * Exemple: "TechCorp Maroc"
     */
    @NotBlank(message = "Le nom de l'entreprise est requis")
    @Size(max = 150, message = "Le nom de l'entreprise ne peut dépasser 150 caractères")
    @Column(nullable = false, length = 150)
    private String entreprise;

    /**
     * Description détaillée du poste
     * Peut inclure: missions, responsabilités, environnement
     */
    @NotBlank(message = "La description est requise")
    @Size(min = 20, max = 5000, message = "La description doit contenir entre 20 et 5000 caractères")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Localisation du poste
     * Exemple: "Oujda", "Casablanca", "Rabat", "Remote"
     */
    @NotBlank(message = "La localisation est requise")
    @Size(max = 100, message = "La localisation ne peut dépasser 100 caractères")
    @Column(nullable = false, length = 100)
    private String localisation;

    /**
     * Type de contrat
     * Valeurs possibles: "CDI", "CDD", "Stage", "Freelance", "Alternance"
     */
    @NotBlank(message = "Le type de contrat est requis")
    @Pattern(
            regexp = "CDI|CDD|Stage|Freelance|Alternance",
            message = "Type de contrat invalide. Valeurs acceptées: CDI, CDD, Stage, Freelance, Alternance"
    )
    @Column(name = "type_contrat", nullable = false, length = 50)
    private String typeContrat;

    /**
     * Niveau d'expérience requis
     * Valeurs: "Débutant", "Junior", "Confirmé", "Senior", "Expert"
     */
    @Size(max = 50, message = "Le niveau d'expérience ne peut dépasser 50 caractères")
    @Column(name = "niveau_experience", length = 50)
    private String niveauExperience;

    /**
     * Salaire minimum proposé (en MAD)
     * Peut être null si non communiqué
     */
    @Min(value = 0, message = "Le salaire minimum ne peut être négatif")
    @Column(name = "salaire_min")
    private Double salaireMin;

    /**
     * Salaire maximum proposé (en MAD)
     * Peut être null si non communiqué
     */
    @Min(value = 0, message = "Le salaire maximum ne peut être négatif")
    @Column(name = "salaire_max")
    private Double salaireMax;

    /**
     * Compétences requises (format texte séparé par virgules)
     * Exemple: "Java 17, Spring Boot, MySQL, Docker, Git"
     *
     * Note: Pour MVP, on utilise TEXT. En production, considérer:
     * - Table séparée competence + Many-to-Many
     * - JSON column pour flexibilité
     */
    @Size(max = 1000, message = "Les compétences ne peuvent dépasser 1000 caractères")
    @Column(columnDefinition = "TEXT")
    private String competences;

    // ============================================
    // SPÉCIFICITÉS OFFRE PRIVÉE
    // ============================================

    /**
     * Étudiant destinataire de cette offre
     *
     * CRITIQUE: Cette relation définit la visibilité
     * - Seul cet étudiant peut voir cette offre
     * - Toutes les queries DOIVENT filtrer par destinataire_id
     * - fetch = LAZY pour éviter N+1 queries
     */
    @NotNull(message = "Le destinataire est requis")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Etudiant destinataire;

    /**
     * Utilisateur émetteur (Admin qui a créé l'offre)
     *
     * Optionnel: Peut être null si création système
     * Utile pour audit trail et support
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emetteur_id")
    private Utilisateur emetteur;

    // ============================================
    // GESTION DU CYCLE DE VIE
    // ============================================

    /**
     * Date de création de l'offre
     *
     * Automatiquement remplie à la création via @PrePersist
     */
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /**
     * Date d'envoi de l'offre au destinataire
     *
     * Peut différer de dateCreation si offre créée en brouillon
     * Pour MVP: identique à dateCreation
     */
    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;

    /**
     * Date d'expiration de l'offre
     *
     * IMPORTANT:
     * - Après cette date, offre automatiquement désactivée
     * - Scheduled task quotidien désactive les offres expirées
     * - Validation: doit être dans le futur
     */
    @NotNull(message = "La date d'expiration est requise")
    @Future(message = "La date d'expiration doit être dans le futur")
    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    /**
     * Indicateur si l'offre est active
     *
     * false = désactivée (expirée, annulée, ou suppression soft)
     * true = active et visible pour le destinataire
     *
     * Queries standards: WHERE active = true
     */
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Indicateur si le destinataire a VU l'offre
     *
     * false = non lue (badge notification frontend)
     * true = lue (étudiant a ouvert le détail)
     *
     * Mis à jour via endpoint PATCH /offres-privees/{id}/mark-read
     */
    @Column(nullable = false)
    private Boolean vue = false;

    /**
     * Date de première lecture (optionnel)
     *
     * Rempli automatiquement quand vue passe de false → true
     * Utile pour analytics (délai lecture)
     */
    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    /**
     * Raison de désactivation (optionnel)
     *
     * Exemples:
     * - "Expirée automatiquement"
     * - "Poste pourvu"
     * - "Annulée par admin"
     * - "Étudiant déjà embauché"
     */
    @Column(name = "raison_desactivation", columnDefinition = "TEXT")
    private String raisonDesactivation;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    /**
     * Hook JPA exécuté avant persist (INSERT)
     *
     * Initialise automatiquement:
     * - dateCreation à l'instant de création
     * - dateEnvoi identique à dateCreation (pour MVP)
     */
    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();

        // Pour MVP: envoi immédiat
        // En production: pourrait avoir workflow brouillon → envoi
        if (this.dateEnvoi == null) {
            this.dateEnvoi = this.dateCreation;
        }
    }

    // ============================================
    // BUSINESS METHODS
    // ============================================

    /**
     * Vérifie si l'offre est expirée
     *
     * @return true si dateExpiration < maintenant
     */
    public boolean isExpired() {
        return this.dateExpiration != null &&
                this.dateExpiration.isBefore(LocalDateTime.now());
    }

    /**
     * Désactive l'offre avec raison
     *
     * @param raison Raison de la désactivation
     */
    public void desactiver(String raison) {
        this.active = false;
        this.raisonDesactivation = (raison != null && !raison.isBlank())
                ? raison.trim()
                : "Désactivée";
    }

    /**
     * Marque l'offre comme lue
     *
     * Idempotent: peut être appelé plusieurs fois sans effet
     */
    public void marquerCommeLue() {
        if (!this.vue) {
            this.vue = true;
            this.dateLecture = LocalDateTime.now();
        }
    }

    /**
     * Vérifie si l'offre appartient à un étudiant
     *
     * @param etudiantId ID de l'étudiant à vérifier
     * @return true si c'est le destinataire
     */
    public boolean appartientA(Long etudiantId) {
        return this.destinataire != null &&
                this.destinataire.getId().equals(etudiantId);
    }
}