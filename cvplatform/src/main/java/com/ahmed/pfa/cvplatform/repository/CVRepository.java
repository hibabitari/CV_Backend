package com.ahmed.pfa.cvplatform.repository;

import com.ahmed.pfa.cvplatform.model.CV;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CVRepository extends JpaRepository<CV, Long> {

    // ✅ Méthode existante (garde-la)
    List<CV> findByEtudiantId(Long etudiantId);

    // ✅ NOUVELLE méthode avec Pagination
    Page<CV> findByEtudiantId(Long etudiantId, Pageable pageable);

    List<CV> findByNomFichier(String nomFichier);

    Long countByEtudiantId(Long etudiantId);

    boolean existsByNomFichierAndEtudiantId(String nomFichier, Long etudiantId);
}