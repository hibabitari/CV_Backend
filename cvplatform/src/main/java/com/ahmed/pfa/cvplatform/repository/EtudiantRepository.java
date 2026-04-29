package com.ahmed.pfa.cvplatform.repository;

import com.ahmed.pfa.cvplatform.model.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    // Trouver un étudiant par email
    Optional<Etudiant> findByEmail(String email);

    // Vérifier si un étudiant existe par email
    boolean existsByEmail(String email);

    // Trouver les étudiants par université
    java.util.List<Etudiant> findByUniversite(String universite);

    // Trouver les étudiants par niveau d'étude
    java.util.List<Etudiant> findByNiveauEtude(String niveauEtude);
}