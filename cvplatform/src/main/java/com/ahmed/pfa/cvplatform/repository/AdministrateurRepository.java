package com.ahmed.pfa.cvplatform.repository;

import com.ahmed.pfa.cvplatform.model.Administrateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdministrateurRepository extends JpaRepository<Administrateur, Long> {

    // Trouver un administrateur par email
    Optional<Administrateur> findByEmail(String email);

    // Vérifier si un admin existe par email
    boolean existsByEmail(String email);
}
