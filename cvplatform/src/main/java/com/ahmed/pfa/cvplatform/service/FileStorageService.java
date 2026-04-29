package com.ahmed.pfa.cvplatform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final String uploadDir;
    private Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir;
        logger.info("📁 FileStorageService initialisé avec: {}", uploadDir);
    }

    @PostConstruct
    public void init() {
        try {
            this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.fileStorageLocation);

            // Test d'écriture
            Path testFile = this.fileStorageLocation.resolve("test.tmp");
            Files.writeString(testFile, "test");
            Files.deleteIfExists(testFile);

            logger.info("✅ Dossier d'upload prêt: {} (writable: {})",
                    this.fileStorageLocation, Files.isWritable(this.fileStorageLocation));
        } catch (IOException ex) {
            logger.error("❌ ÉCHEC CRITIQUE - Impossible d'initialiser le stockage: {}", uploadDir, ex);
            throw new RuntimeException("Impossible d'initialiser le dossier d'upload: " + uploadDir, ex);
        }
    }

    public String storeFile(MultipartFile file) {
        logger.info("📤 Stockage du fichier: {} ({} bytes)", file.getOriginalFilename(), file.getSize());

        try {
            // Validation
            if (file.isEmpty()) {
                throw new IOException("Fichier vide");
            }

            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.trim().isEmpty()) {
                throw new IOException("Nom de fichier invalide");
            }

            // Générer nom unique
            String fileExtension = "";
            int dotIndex = originalFileName.lastIndexOf(".");
            if (dotIndex > 0) {
                fileExtension = originalFileName.substring(dotIndex).toLowerCase();
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;

            // Chemin complet
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            logger.debug("📍 Chemin cible: {}", targetLocation);

            // Copie du fichier
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Vérification
            if (!Files.exists(targetLocation)) {
                throw new IOException("Échec de création du fichier sur disque");
            }

            long fileSize = Files.size(targetLocation);
            logger.info("✅ Fichier stocké: {} ({} bytes)", fileName, fileSize);

            return fileName;

        } catch (IOException ex) {
            logger.error("❌ Échec stockage fichier: {}", file.getOriginalFilename(), ex);
            throw new RuntimeException("Erreur stockage fichier: " + ex.getMessage(), ex);
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

            // Sécurité: vérifier que le fichier est dans le dossier d'upload
            if (!filePath.startsWith(this.fileStorageLocation)) {
                logger.warn("⚠️ Tentative de suppression hors dossier: {}", fileName);
                return;
            }

            boolean deleted = Files.deleteIfExists(filePath);
            logger.info("🗑️ Fichier supprimé: {} (existant: {})", fileName, deleted);

        } catch (IOException ex) {
            logger.error("❌ Erreur suppression fichier: {}", fileName, ex);
            // Ne pas propager l'erreur - on continue la suppression en BDD
        }
    }

    public Path getFilePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName).normalize();
    }

    /**
     * Lit le contenu d'un fichier stocké (nom interne UUID), avec contrôle de chemin.
     */
    public byte[] loadFileBytes(String storedFileName) throws IOException {
        if (storedFileName == null || storedFileName.isBlank()) {
            throw new IOException("Nom de fichier stocké invalide");
        }
        Path filePath = this.fileStorageLocation.resolve(storedFileName).normalize();
        if (!filePath.startsWith(this.fileStorageLocation)) {
            logger.warn("⚠️ Tentative de lecture hors dossier: {}", storedFileName);
            throw new SecurityException("Chemin fichier invalide");
        }
        if (!Files.exists(filePath)) {
            throw new IOException("Fichier introuvable sur disque: " + storedFileName);
        }
        return Files.readAllBytes(filePath);
    }
}