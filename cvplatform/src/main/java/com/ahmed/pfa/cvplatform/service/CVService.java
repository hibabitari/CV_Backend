package com.ahmed.pfa.cvplatform.service;

import com.ahmed.pfa.cvplatform.dto.CVDownloadResult;
import com.ahmed.pfa.cvplatform.dto.CVResponse;
import com.ahmed.pfa.cvplatform.dto.CVUploadResponse;
import com.ahmed.pfa.cvplatform.exception.InvalidFileException;
import com.ahmed.pfa.cvplatform.exception.ResourceNotFoundException;
import com.ahmed.pfa.cvplatform.model.CV;
import com.ahmed.pfa.cvplatform.model.Etudiant;
import com.ahmed.pfa.cvplatform.repository.CVRepository;
import com.ahmed.pfa.cvplatform.repository.EtudiantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CVService {

    private static final Logger logger = LoggerFactory.getLogger(CVService.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".pdf", ".docx", ".doc");

    @Autowired
    private CVRepository cvRepository;

    @Autowired
    private EtudiantRepository etudiantRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private CvTextExtractionService cvTextExtractionService;

    @Transactional
    public CVUploadResponse uploadCV(MultipartFile file, Long etudiantId) {
        logger.info("🚀 UPLOAD CV - etudiantId: {}, fichier: {}, taille: {} bytes",
                etudiantId, file.getOriginalFilename(), file.getSize());

        String storedFileName = null;

        try {
            // 1. Validation étudiant
            Etudiant etudiant = etudiantRepository.findById(etudiantId)
                    .orElseThrow(() -> {
                        logger.error("❌ Étudiant non trouvé: id={}", etudiantId);
                        return new ResourceNotFoundException("Étudiant", etudiantId);
                    });
            logger.debug("✅ Étudiant trouvé: {} {}", etudiant.getNom(), etudiant.getPrenom());

            // 2. Validation fichier
            validateFile(file);

            // 3. Stockage physique
            storedFileName = fileStorageService.storeFile(file);
            logger.debug("💾 Fichier stocké: {}", storedFileName);

            // 4. Création entité CV
            CV cv = new CV();
            cv.setNomFichier(file.getOriginalFilename());
            cv.setCheminFichier(storedFileName);
            cv.setTypeFichier(file.getContentType());
            cv.setTailleFichier(file.getSize());
            cv.setDateUpload(LocalDateTime.now());
            cv.setEtudiant(etudiant);

            try {
                byte[] storedBytes = fileStorageService.loadFileBytes(storedFileName);
                String extrait = cvTextExtractionService.extractPlainText(
                        storedBytes,
                        file.getOriginalFilename(),
                        file.getContentType());
                if (extrait != null && !extrait.isBlank()) {
                    cv.setContenuTexte(extrait);
                    logger.debug("Texte extrait: {} caractères", extrait.length());
                } else {
                    logger.debug("Aucun texte extrait pour {}", file.getOriginalFilename());
                }
            } catch (Exception ex) {
                logger.warn("Extraction contenu_texte ignorée (upload continue): {}", ex.getMessage());
            }

            // 5. Sauvegarde BDD
            CV savedCV = cvRepository.save(cv);
            logger.info("✅ CV uploadé avec succès: id={}, fileName={}", savedCV.getId(), storedFileName);

            return new CVUploadResponse(
                    savedCV.getId(),
                    savedCV.getNomFichier(),
                    savedCV.getTypeFichier(),
                    savedCV.getTailleFichier(),
                    savedCV.getDateUpload(),
                    "CV uploadé avec succès"
            );

        } catch (ResourceNotFoundException | InvalidFileException e) {
            // Exceptions métier - rollback automatique
            logger.warn("⚠️ Erreur métier upload: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Cleanup en cas d'erreur inattendue
            if (storedFileName != null) {
                try {
                    fileStorageService.deleteFile(storedFileName);
                    logger.debug("🗑️ Fichier orphelin supprimé: {}", storedFileName);
                } catch (Exception cleanupEx) {
                    logger.warn("⚠️ Impossible de supprimer le fichier orphelin: {}", storedFileName);
                }
            }
            logger.error("❌ ERREUR CRITIQUE UPLOAD:", e);
            throw new RuntimeException("Erreur lors de l'upload du CV: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        // Fichier vide
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Fichier vide ou absent");
        }

        // Taille
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("Fichier trop volumineux. Maximum 5MB.");
        }

        // Type MIME
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        boolean validType = (contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType)) ||
                (fileName != null && ALLOWED_EXTENSIONS.stream()
                        .anyMatch(ext -> fileName.toLowerCase().endsWith(ext)));

        if (!validType) {
            throw new InvalidFileException("Format non supporté. Utilisez PDF ou Word (.pdf, .docx)");
        }

        logger.debug("✅ Validation fichier OK: {} ({})", fileName, contentType);
    }

    @Transactional
    public void deleteCV(Long cvId) {
        logger.info("🗑️ Suppression CV: id={}", cvId);

        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> {
                    logger.error("❌ CV non trouvé: id={}", cvId);
                    return new ResourceNotFoundException("CV", cvId);
                });

        // Supprimer fichier physique (même si échec, on continue pour la BDD)
        try {
            fileStorageService.deleteFile(cv.getCheminFichier());
        } catch (Exception e) {
            logger.warn("⚠️ Erreur suppression fichier (continuation): {}", cv.getCheminFichier());
        }

        // Supprimer BDD
        cvRepository.delete(cv);
        logger.info("✅ CV supprimé: id={}", cvId);
    }

    @Transactional(readOnly = true)
    public Page<CVResponse> getCVsByEtudiantPage(Long etudiantId, int page, int size) {
        logger.debug("📄 Récupération paginée CVs - etudiantId: {}, page: {}, size: {}", etudiantId, page, size);

        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("dateUpload").descending());
        return cvRepository.findByEtudiantId(etudiantId, pageable).map(this::mapToCVResponse);
    }

    @Transactional(readOnly = true)
    public List<CVResponse> getCVsByEtudiant(Long etudiantId) {
        logger.debug("📋 Récupération tous CVs - etudiantId: {}", etudiantId);
        return cvRepository.findByEtudiantId(etudiantId).stream()
                .map(this::mapToCVResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CVResponse getCVById(Long cvId) {
        logger.debug("🔍 Recherche CV: id={}", cvId);
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("CV", cvId));
        return mapToCVResponse(cv);
    }

    /**
     * Téléchargement du fichier CV : propriétaire (étudiant) ou administrateur.
     */
    @Transactional(readOnly = true)
    public CVDownloadResult getCVDownload(Long cvId, Long requesterUserId, boolean requesterIsAdmin) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResourceNotFoundException("CV", cvId));
        Long ownerId = cv.getEtudiant().getId();
        if (!requesterIsAdmin && !ownerId.equals(requesterUserId)) {
            throw new AccessDeniedException("Accès au fichier CV refusé");
        }
        byte[] bytes;
        try {
            bytes = fileStorageService.loadFileBytes(cv.getCheminFichier());
        } catch (IOException e) {
            logger.error("❌ Lecture fichier CV id={}: {}", cvId, e.getMessage());
            throw new ResourceNotFoundException("Fichier CV sur disque introuvable ou illisible");
        }
        String contentType = cv.getTypeFichier();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return new CVDownloadResult(cv.getNomFichier(), contentType, bytes);
    }

    private CVResponse mapToCVResponse(CV cv) {
        return new CVResponse(
                cv.getId(),
                cv.getNomFichier(),
                cv.getTypeFichier(),
                cv.getTailleFichier(),
                cv.getDateUpload(),
                cv.getEtudiant().getId(),
                cv.getEtudiant().getNom() + " " + cv.getEtudiant().getPrenom(),
                false
        );
    }
}