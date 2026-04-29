package com.ahmed.pfa.cvplatform.service;

import com.ahmed.pfa.cvplatform.dto.RegisterRequest;
import com.ahmed.pfa.cvplatform.dto.UpdateProfileRequest;
import com.ahmed.pfa.cvplatform.dto.UserProfileResponse;
import com.ahmed.pfa.cvplatform.exception.UserNotFoundException;
import com.ahmed.pfa.cvplatform.model.Administrateur;
import com.ahmed.pfa.cvplatform.model.Etudiant;
import com.ahmed.pfa.cvplatform.model.Utilisateur;
import com.ahmed.pfa.cvplatform.repository.AdministrateurRepository;
import com.ahmed.pfa.cvplatform.repository.EtudiantRepository;
import com.ahmed.pfa.cvplatform.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private EtudiantRepository etudiantRepository;

    @Autowired
    private AdministrateurRepository administrateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // =========================================================================
    // ✅ MÉTHODE D'INSCRIPTION (CORRIGÉE)
    // =========================================================================

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        // Validation des mots de passe
        /*if (!request.getMotDePasse().equals(request.getConfirmMotDePasse())) {
            throw new IllegalArgumentException("Les mots de passe ne correspondent pas");
        }*/

        // Vérifier si l'email existe déjà
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        // Sauvegarde selon le rôle
        if ("ETUDIANT".equalsIgnoreCase(request.getRole())) {
            Etudiant etudiant = new Etudiant();
            etudiant.setNom(request.getNom());
            etudiant.setPrenom(request.getPrenom());
            etudiant.setEmail(request.getEmail());
            etudiant.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
            etudiant.setRole(request.getRole());
            etudiant.setTypeUtilisateur("ETUDIANT");
            etudiant.setDateCreation(LocalDateTime.now());
            // Champs spécifiques étudiant
            etudiant.setNiveauEtude(request.getNiveauEtude());
            etudiant.setDomaineEtude(request.getDomaineEtude());
            etudiant.setUniversite(request.getUniversite());
            etudiantRepository.save(etudiant);
            return mapToProfileResponse(etudiant);
        } else if ("ADMIN".equalsIgnoreCase(request.getRole())) {
            Administrateur admin = new Administrateur();
            admin.setNom(request.getNom());
            admin.setPrenom(request.getPrenom());
            admin.setEmail(request.getEmail());
            admin.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
            admin.setRole(request.getRole());
            admin.setTypeUtilisateur("ADMIN");
            admin.setDateCreation(LocalDateTime.now());
            // Champs spécifiques admin (optionnels)
            admin.setPermissions(String.valueOf(List.of("READ", "WRITE", "DELETE")));
            administrateurRepository.save(admin);
            return mapToProfileResponse(admin);
        } else {
            // Utilisateur générique
            Utilisateur user = new Utilisateur();
            user.setNom(request.getNom());
            user.setPrenom(request.getPrenom());
            user.setEmail(request.getEmail());
            user.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
            user.setRole(request.getRole());
            user.setTypeUtilisateur(request.getRole());
            user.setDateCreation(LocalDateTime.now());
            utilisateurRepository.save(user);
            return mapToProfileResponse(user);
        }
    }

    // =========================================================================
    // ✅ MÉTHODES DE LECTURE AVEC PAGINATION
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsersPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Utilisateur> usersPage = utilisateurRepository.findAll(pageable);
        return usersPage.map(this::mapToProfileResponse);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return mapToProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return utilisateurRepository.findAll().stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // ✅ OPÉRATIONS DE MISE À JOUR ET SUPPRESSION
    // =========================================================================

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.getNom() != null) user.setNom(request.getNom());
        if (request.getPrenom() != null) user.setPrenom(request.getPrenom());

        if (user instanceof Etudiant etudiant) {
            if (request.getNiveauEtude() != null) etudiant.setNiveauEtude(request.getNiveauEtude());
            if (request.getDomaineEtude() != null) etudiant.setDomaineEtude(request.getDomaineEtude());
            if (request.getUniversite() != null) etudiant.setUniversite(request.getUniversite());
            etudiantRepository.save(etudiant);
        } else if (user instanceof Administrateur admin) {
            if (request.getPermissions() != null) admin.setPermissions(request.getPermissions());
            administrateurRepository.save(admin);
        } else {
            utilisateurRepository.save(user);
        }

        return mapToProfileResponse(user);
    }

    @Transactional
    public void deleteUserAsAdmin(Long adminUserId, Long targetUserId) {
        if (adminUserId != null && adminUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Un administrateur ne peut pas se supprimer lui-même");
        }
        if (!utilisateurRepository.existsById(targetUserId)) {
            throw new UserNotFoundException(targetUserId);
        }
        utilisateurRepository.deleteById(targetUserId);
    }

    // =========================================================================
    // ✅ MAPPER PRIVÉ
    // =========================================================================

    private UserProfileResponse mapToProfileResponse(Utilisateur user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setNom(user.getNom());
        response.setPrenom(user.getPrenom());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setDateCreation(user.getDateCreation());

        if (user instanceof Etudiant etudiant) {
            response.setNiveauEtude(etudiant.getNiveauEtude());
            response.setDomaineEtude(etudiant.getDomaineEtude());
            response.setUniversite(etudiant.getUniversite());
        } else if (user instanceof Administrateur admin) {
            response.setPermissions(admin.getPermissions());
        }

        return response;
    }
}