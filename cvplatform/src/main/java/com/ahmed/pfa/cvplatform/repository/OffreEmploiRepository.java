package com.ahmed.pfa.cvplatform.repository;

import com.ahmed.pfa.cvplatform.model.OffreEmploi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OffreEmploiRepository extends JpaRepository<OffreEmploi, Long> {

    // ✅ Méthodes existantes (garde-les)
    List<OffreEmploi> findByActiveTrue();
    List<OffreEmploi> findByEntreprise(String entreprise);
    List<OffreEmploi> findByLocalisation(String localisation);
    List<OffreEmploi> findByTypeContrat(String typeContrat);
    List<OffreEmploi> findByEtudiantId(Long etudiantId);
    List<OffreEmploi> findByEtudiantIdAndActiveTrue(Long etudiantId);

    List<OffreEmploi> findByActiveTrueAndEtudiantIsNull();
    List<OffreEmploi> findByLocalisationAndEtudiantIsNull(String localisation);
    List<OffreEmploi> findByTypeContratAndEtudiantIsNull(String typeContrat);
    List<OffreEmploi> findByLocalisationAndEtudiantId(String localisation, Long etudiantId);
    List<OffreEmploi> findByTypeContratAndEtudiantId(String typeContrat, Long etudiantId);

    @Query("SELECT o FROM OffreEmploi o WHERE LOWER(o.titre) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<OffreEmploi> searchByTitre(@Param("keyword") String keyword);

    @Query("SELECT o FROM OffreEmploi o WHERE " +
            "LOWER(o.titre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.entreprise) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<OffreEmploi> searchGlobal(@Param("keyword") String keyword);

    @Query("SELECT o FROM OffreEmploi o WHERE o.etudiant IS NULL AND " +
            "(LOWER(o.titre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(o.entreprise) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(o.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<OffreEmploi> searchAdmin(@Param("keyword") String keyword);

    @Query("SELECT o FROM OffreEmploi o WHERE o.etudiant.id = :etudiantId AND " +
            "(LOWER(o.titre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(o.entreprise) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(o.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<OffreEmploi> searchStudent(@Param("keyword") String keyword, @Param("etudiantId") Long etudiantId);

    // ✅ NOUVELLES méthodes avec Pagination
    Page<OffreEmploi> findByActiveTrue(Pageable pageable);

    Page<OffreEmploi> findByLocalisation(String localisation, Pageable pageable);

    Page<OffreEmploi> findByTypeContrat(String typeContrat, Pageable pageable);
}