package com.ahmed.pfa.cvplatform.controller;

import com.ahmed.pfa.cvplatform.dto.CVResponse;
import com.ahmed.pfa.cvplatform.dto.CVUploadResponse;
import com.ahmed.pfa.cvplatform.dto.PagedResponse;
import com.ahmed.pfa.cvplatform.service.CVService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "CV Management", description = "Gestion des CV")
@RestController
@RequestMapping("/api/cv")
@SecurityRequirement(name = "bearer-jwt")
public class CVController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(CVController.class);

    @Autowired
    private CVService cvService;

    @PostMapping("/upload")
    @Operation(summary = "Télécharger un CV")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Succès"),
            @ApiResponse(responseCode = "400", description = "Fichier invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Étudiant non trouvé")
    })
    public ResponseEntity<CVUploadResponse> uploadCV(
            @Parameter(description = "Fichier CV", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "ID étudiant", required = true)
            @RequestParam("etudiantId") Long etudiantId) {

        String userEmail = "unknown";
        try {
            userEmail = getAuthenticatedUserEmail();
        } catch (Exception e) {
            logger.debug("Email utilisateur non disponible");
        }

        logger.info("📥 Upload CV demandé par {} pour étudiant {}", userEmail, etudiantId);

        CVUploadResponse response = cvService.uploadCV(file, etudiantId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un CV")
    public ResponseEntity<Void> deleteCV(@PathVariable Long id) {
        logger.info("🗑️ Suppression CV: id={}", id);
        cvService.deleteCV(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/etudiant/{etudiantId}/page")
    @Operation(summary = "CVs paginés d'un étudiant")
    public ResponseEntity<PagedResponse<CVResponse>> getCVsByEtudiantPage(
            @PathVariable Long etudiantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int pageSize = Math.min(Math.max(size, 1), 50);
        logger.debug("📄 CVs paginés - etudiantId: {}, page: {}, size: {}", etudiantId, page, pageSize);

        Page<CVResponse> cvsPage = cvService.getCVsByEtudiantPage(etudiantId, page, pageSize);
        return ResponseEntity.ok(PagedResponse.of(cvsPage));
    }

    @GetMapping("/etudiant/{etudiantId}")
    @Operation(summary = "Tous les CVs d'un étudiant")
    public ResponseEntity<List<CVResponse>> getCVsByEtudiant(@PathVariable Long etudiantId) {
        logger.debug("📋 CVs étudiant: {}", etudiantId);
        return ResponseEntity.ok(cvService.getCVsByEtudiant(etudiantId));
    }

    @GetMapping("/test")
    @Operation(summary = "Test endpoint")
    public String test() {
        return "CV Controller OK - " + java.time.LocalDateTime.now();
    }

    @GetMapping("/{id}")
    @Operation(summary = "CV par ID")
    public ResponseEntity<CVResponse> getCVById(@PathVariable Long id) {
        logger.debug("🔍 CV par ID: {}", id);
        return ResponseEntity.ok(cvService.getCVById(id));
    }
}