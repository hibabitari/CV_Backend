package com.ahmed.pfa.cvplatform.config;

import org.springframework.beans.factory.annotation.Value;

/**
 * Custom health indicator pour file storage
 * Temporairement désactivé - IntelliJ indexing issue
 */
// @Component  // ← Commenté pour éviter erreurs IntelliJ
public class FileStorageHealthIndicator {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    // Health indicator temporairement désactivé
    // Actuator fournira quand même les health checks par défaut (db, diskSpace, ping)
}
