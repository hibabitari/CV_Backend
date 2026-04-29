package com.ahmed.pfa.cvplatform.repository;

import com.ahmed.pfa.cvplatform.model.AnalyseIA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les analyses IA
 */
@Repository
public interface AnalyseIARepository extends JpaRepository<AnalyseIA, Long> {

    /**
     * Trouver toutes les analyses d'un CV
     */
    List<AnalyseIA> findByCvId(Long cvId);

    /**
     * Trouver toutes les analyses pour une offre
     */
    List<AnalyseIA> findByOffreEmploiId(Long offreEmploiId);

    /**
     * Trouver analyse par CV et Offre
     */
    Optional<AnalyseIA> findByCvIdAndOffreEmploiId(Long cvId, Long offreEmploiId);

    /**
     * Trouver analyses par statut
     */
    List<AnalyseIA> findByStatut(AnalyseIA.StatutAnalyse statut);

    /**
     * Compter analyses d'un CV
     */
    Long countByCvId(Long cvId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AnalyseIA a WHERE a.offreEmploi.id = :offreId")
    int deleteAllForOffreEmploiId(@Param("offreId") Long offreId);
}