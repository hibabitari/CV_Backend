package com.ahmed.pfa.cvplatform.repository;

import com.ahmed.pfa.cvplatform.model.OffrePrivee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'accès aux données des offres privées
 *
 * PRINCIPES DE SÉCURITÉ:
 * - Toutes les méthodes de lecture incluent destinataire_id
 * - Pas de findAll() global (évite data leak)
 * - Queries explicites pour chaque cas d'usage
 *
 * NAMING CONVENTION:
 * - findBy... : Retourne List ou Optional
 * - countBy... : Retourne Long
 * - existsBy... : Retourne boolean
 * - deleteBy... : Suppression
 *
 * @author Ahmed
 * @version 1.0
 */
@Repository
public interface OffrePriveeRepository extends JpaRepository<OffrePrivee, Long> {

    // ============================================
    // QUERIES ÉTUDIANT (Filtered by destinataire)
    // ============================================

    /**
     * Récupère toutes les offres ACTIVES d'un étudiant
     *
     * USE CASE: Étudiant voit ses offres privées
     * SÉCURITÉ: Filtré par destinataire_id
     *
     * @param etudiantId ID du destinataire
     * @return Liste des offres actives
     */
    List<OffrePrivee> findByDestinataire_IdAndActiveTrue(Long etudiantId);

    /**
     * Récupère les offres actives d'un étudiant avec pagination
     *
     * USE CASE: GET /api/offres-privees/me?page=0&size=20
     * PERFORMANCE: Pageable pour grandes listes
     *
     * @param etudiantId ID du destinataire
     * @param pageable Paramètres de pagination
     * @return Page d'offres actives
     */
    Page<OffrePrivee> findByDestinataire_IdAndActiveTrue(
            Long etudiantId,
            Pageable pageable
    );

    /**
     * Récupère une offre par ID ET destinataire (ownership check)
     *
     * USE CASE: GET /api/offres-privees/{id}
     * SÉCURITÉ CRITIQUE: Valide que l'offre appartient à l'étudiant
     *
     * Si offre existe mais destinataire différent → empty Optional
     * → Controller retourne 404 (pas 403, évite enumeration)
     *
     * @param id ID de l'offre
     * @param etudiantId ID du destinataire
     * @return Optional<OffrePrivee> (empty si pas owner)
     */
    Optional<OffrePrivee> findByIdAndDestinataire_Id(Long id, Long etudiantId);

    /**
     * Compte les offres NON LUES d'un étudiant
     *
     * USE CASE: Badge notification frontend
     * GET /api/offres-privees/me/count-unread → { "count": 3 }
     *
     * @param etudiantId ID du destinataire
     * @return Nombre d'offres actives non vues
     */
    Long countByDestinataire_IdAndActiveTrueAndVueFalse(Long etudiantId);

    /**
     * Compte toutes les offres actives d'un étudiant
     *
     * USE CASE: Stats dashboard étudiant
     *
     * @param etudiantId ID du destinataire
     * @return Nombre total d'offres actives
     */
    Long countByDestinataire_IdAndActiveTrue(Long etudiantId);

    /**
     * Vérifie si une offre existe et appartient à un étudiant
     *
     * USE CASE: Validation rapide avant opération
     * Plus performant que findByIdAndDestinataire_Id si on n'a pas besoin de l'entity
     *
     * @param id ID de l'offre
     * @param etudiantId ID du destinataire
     * @return true si offre existe ET appartient à l'étudiant
     */
    boolean existsByIdAndDestinataire_Id(Long id, Long etudiantId);

    // ============================================
    // QUERIES ADMIN (Gestion globale)
    // ============================================

    /**
     * Toutes les offres privées envoyées à un étudiant (admin : actives + expirées).
     */
    List<OffrePrivee> findByDestinataire_IdOrderByDateCreationDesc(Long destinataireId);

    /**
     * Récupère toutes les offres (admin only)
     *
     * USE CASE: Dashboard admin, vue globale
     * ATTENTION: Ne pas exposer directement aux étudiants
     *
     * @param pageable Paramètres de pagination
     * @return Page de toutes les offres
     */
    Page<OffrePrivee> findAll(Pageable pageable);

    /**
     * Récupère toutes les offres actives (admin)
     *
     * @return Liste des offres actives globalement
     */
    List<OffrePrivee> findByActiveTrue();

    /**
     * Compte toutes les offres par statut (admin stats)
     *
     * @param active Statut actif
     * @return Nombre d'offres avec ce statut
     */
    Long countByActive(Boolean active);

    /**
     * Compte toutes les offres non vues (admin stats)
     *
     * @param vue Statut vu
     * @return Nombre d'offres avec ce statut
     */
    Long countByVue(Boolean vue);

    // ============================================
    // QUERIES CLEANUP (Scheduled tasks)
    // ============================================

    /**
     * Trouve les offres expirées ACTIVES (pour désactivation)
     *
     * USE CASE: Scheduled task quotidien
     * @Scheduled(cron = "0 0 2 * * *")
     *
     * Query: WHERE date_expiration < :now AND active = true
     *
     * @param dateLimit Date limite (généralement LocalDateTime.now())
     * @return Liste des offres à désactiver
     */
    List<OffrePrivee> findByDateExpirationBeforeAndActiveTrue(LocalDateTime dateLimit);

    /**
     * Trouve les offres INACTIVES anciennes (pour hard delete)
     *
     * USE CASE: Purge mensuelle
     * @Scheduled(cron = "0 0 3 1 * *") // 1er du mois
     *
     * Query: WHERE date_expiration < :cutoff AND active = false
     * Exemple: cutoff = now - 3 months
     *
     * @param cutoffDate Date de coupure (ex: 3 mois avant)
     * @return Liste des offres à supprimer définitivement
     */
    List<OffrePrivee> findByDateExpirationBeforeAndActiveFalse(LocalDateTime cutoffDate);

    // ============================================
    // CUSTOM QUERIES (Advanced use cases)
    // ============================================

    /**
     * Recherche dans les offres d'un étudiant par mot-clé
     *
     * USE CASE: Étudiant filtre ses offres
     * GET /api/offres-privees/me/search?q=java
     *
     * Recherche dans: titre, entreprise, description, competences
     *
     * @param etudiantId ID du destinataire
     * @param keyword Mot-clé (insensible à la casse)
     * @return Liste des offres matchant
     */
    @Query("""
        SELECT o FROM OffrePrivee o 
        WHERE o.destinataire.id = :etudiantId 
        AND o.active = true
        AND (
            LOWER(o.titre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(o.entreprise) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(o.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(o.competences) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        ORDER BY o.dateCreation DESC
    """)
    List<OffrePrivee> searchByKeyword(
            @Param("etudiantId") Long etudiantId,
            @Param("keyword") String keyword
    );

    /**
     * Récupère les offres récentes d'un étudiant (7 derniers jours)
     *
     * USE CASE: Section "Nouvelles offres" dashboard
     *
     * @param etudiantId ID du destinataire
     * @param since Date limite (ex: now - 7 days)
     * @return Liste des offres créées après 'since'
     */
    @Query("""
        SELECT o FROM OffrePrivee o 
        WHERE o.destinataire.id = :etudiantId 
        AND o.active = true
        AND o.dateCreation >= :since
        ORDER BY o.dateCreation DESC
    """)
    List<OffrePrivee> findRecentOffres(
            @Param("etudiantId") Long etudiantId,
            @Param("since") LocalDateTime since
    );

    /**
     * Compte les offres créées pour un étudiant dans une période
     *
     * USE CASE: Analytics admin - activité par étudiant
     *
     * @param etudiantId ID du destinataire
     * @param start Début période
     * @param end Fin période
     * @return Nombre d'offres créées dans la période
     */
    @Query("""
        SELECT COUNT(o) FROM OffrePrivee o 
        WHERE o.destinataire.id = :etudiantId 
        AND o.dateCreation BETWEEN :start AND :end
    """)
    Long countOffresInPeriod(
            @Param("etudiantId") Long etudiantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Récupère les offres d'un émetteur spécifique
     *
     * USE CASE: Admin voit les offres qu'il a créées
     *
     * @param emetteurId ID de l'émetteur (admin)
     * @param pageable Pagination
     * @return Page des offres créées par cet émetteur
     */
    Page<OffrePrivee> findByEmetteur_Id(Long emetteurId, Pageable pageable);
}