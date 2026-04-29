package com.ahmed.pfa.cvplatform.repository;

import com.ahmed.pfa.cvplatform.model.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    // ✅ Méthodes existantes (garde-les)
    Utilisateur findByEmail(String email);
    boolean existsByEmail(String email);

    // ✅ NOUVELLE méthode avec Pagination
    Page<Utilisateur> findAll(Pageable pageable);
}