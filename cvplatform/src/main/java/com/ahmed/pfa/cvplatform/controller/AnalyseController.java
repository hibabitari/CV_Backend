package com.ahmed.pfa.cvplatform.controller;

import com.ahmed.pfa.cvplatform.dto.AnalyseRequest;
import com.ahmed.pfa.cvplatform.dto.AnalyseResponse;
import com.ahmed.pfa.cvplatform.service.AnalyseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour les analyses IA
 */
@RestController
@RequestMapping("/api/analyses")
public class AnalyseController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyseController.class);

    @Autowired
    private AnalyseService analyseService;

    /**
     * Lancer une nouvelle analyse IA
     *
     * POST /api/analyses
     */
    @PostMapping
    public ResponseEntity<AnalyseResponse> lancerAnalyse(
            @Valid @RequestBody AnalyseRequest request) {

        String currentUser = getAuthenticatedUserEmail();
        logger.info("User {} lance analyse: cvId={}, offreId={}",
                currentUser, request.getCvId(), request.getOffreEmploiId());

        AnalyseResponse response = analyseService.lancerAnalyse(
                request.getCvId(),
                request.getOffreEmploiId()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer une analyse par ID
     *
     * GET /api/analyses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnalyseResponse> getAnalyse(@PathVariable Long id) {
        logger.debug("Récupération analyse: id={}", id);

        AnalyseResponse response = analyseService.getAnalyse(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer toutes les analyses d'un CV
     *
     * GET /api/analyses/cv/{cvId}
     */
    @GetMapping("/cv/{cvId}")
    public ResponseEntity<List<AnalyseResponse>> getAnalysesByCv(@PathVariable Long cvId) {
        logger.debug("Récupération analyses pour CV: cvId={}", cvId);

        List<AnalyseResponse> analyses = analyseService.getAnalysesByCv(cvId);
        return ResponseEntity.ok(analyses);
    }

    /**
     * Test endpoint (dev only)
     */
    @GetMapping("/test")
    public String test() {
        return "Analyse Controller fonctionne!";
    }
}