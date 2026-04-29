package com.ahmed.pfa.cvplatform.repository;

import com.ahmed.pfa.cvplatform.model.Recommandation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour les recommandations
 */
@Repository
public interface RecommandationRepository extends JpaRepository<Recommandation, Long> {

    /**
     * Trouver toutes les recommandations d'une analyse
     */
    List<Recommandation> findByAnalyseIAId(Long analyseIAId);

    /**
     * Trouver recommandations par priorité
     */
    List<Recommandation> findByAnalyseIAIdOrderByPrioriteAsc(Long analyseIAId);

    /**
     * Trouver recommandations par type
     */
    List<Recommandation> findByAnalyseIAIdAndType(Long analyseIAId, Recommandation.TypeRecommandation type);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Recommandation r WHERE r.analyseIA.id IN (SELECT a.id FROM AnalyseIA a WHERE a.offreEmploi.id = :offreId)")
    int deleteAllForOffreEmploiId(@Param("offreId") Long offreId);
}