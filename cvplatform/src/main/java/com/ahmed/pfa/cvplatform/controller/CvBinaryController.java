package com.ahmed.pfa.cvplatform.controller;

import com.ahmed.pfa.cvplatform.dto.CVDownloadResult;
import com.ahmed.pfa.cvplatform.service.CVService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Téléchargement / affichage du fichier CV (binaire).
 * Chemin dédié pour éviter tout conflit de patterns avec {@link CVController} (/api/cv/...).
 */
@Tag(name = "CV Fichier", description = "Binaire CV (propriétaire ou admin)")
@RestController
@RequestMapping("/api/cv-binaire")
@SecurityRequirement(name = "bearer-jwt")
public class CvBinaryController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(CvBinaryController.class);

    @Autowired
    private CVService cvService;

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir le fichier CV (inline)")
    public ResponseEntity<byte[]> getCvFile(@PathVariable Long id) {
        logger.info("📄 CV binaire id={} — {}", id, getAuthenticatedUserEmail());
        CVDownloadResult result = cvService.getCVDownload(id, getCurrentUserId(), isCurrentUserAdmin());
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(result.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }
}
