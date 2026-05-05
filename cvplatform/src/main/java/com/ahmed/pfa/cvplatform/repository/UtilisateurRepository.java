package com.ahmed.pfa.cvplatform.repository;

import com.ahmed.pfa.cvplatform.model.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    //  Méthodes existantes
    Utilisateur findByEmail(String email);
    boolean existsByEmail(String email);
    Page<Utilisateur> findAll(Pageable pageable);

    //  Nouvelle requête pour obtenir les stats d'analyse d'un étudiant
    @Query("SELECT COUNT(a) as total, AVG(a.score) as moyenne FROM AnalyseIA a WHERE a.cv.etudiant.id = :id AND a.statut = 'TERMINEE'")
    Map<String, Object> getStudentStats(@Param("id") Long id);
}