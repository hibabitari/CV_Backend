package com.ahmed.pfa.cvplatform.service;

import com.ahmed.pfa.cvplatform.dto.AIAnalysisResult;
import com.ahmed.pfa.cvplatform.exception.IAServiceException;
import com.ahmed.pfa.cvplatform.exception.IAServiceTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * AI Client Service with Timeout Protection
 *
 * Calls external IA API (Python backend) with comprehensive error handling:
 * - Connection timeout: 5 seconds
 * - Read timeout: 30 seconds
 * - Automatic exception handling
 * - Detailed logging
 *
 * MODES:
 * - MOCK: Simulates IA responses (for development)
 * - REAL: Calls actual Python IA API (for production)
 *
 * @author Ahmed
 */
@Service
public class AIClientService {

    private static final Logger logger = LoggerFactory.getLogger(AIClientService.class);

    @Autowired
    @Qualifier("iaServiceRestTemplate")
    private RestTemplate restTemplate;

    @Value("${ia.api.url:http://localhost:5000/analyze}")
    private String iaApiUrl;

    @Value("${ia.api.enabled:false}")
    private boolean iaApiEnabled;

    @Value("${ia.api.timeout.seconds:30}")
    private int timeoutSeconds;

    @Value("${ia.mock.enabled:true}")
    private boolean mockEnabled;

    /**
     * Analyze CV against job description
     *
     * @param cvText CV content
     * @param jobDescription Job offer description
     * @return Analysis result
     * @throws IAServiceTimeoutException if request times out
     * @throws IAServiceException if IA service fails
     */
    public AIAnalysisResult analyzeCV(String cvText, String jobDescription) {
        logger.info("Starting CV analysis - Mode: {}, API Enabled: {}",
                mockEnabled ? "MOCK" : "REAL", iaApiEnabled);

        long startTime = System.currentTimeMillis();

        try {
            AIAnalysisResult result;

            if (mockEnabled) {
                // MOCK MODE: Simulate IA response
                result = generateMockAnalysis(cvText, jobDescription);
            } else {
                // REAL MODE: Call actual IA API with timeout protection
                result = callRealIAApi(cvText, jobDescription);
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("CV analysis completed in {}ms - Score: {:.2f}%",
                    duration, result.getScore());

            return result;

        } catch (ResourceAccessException ex) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("IA API timeout after {}ms", duration, ex);

            throw new IAServiceTimeoutException(
                    "L'analyse IA a dépassé le délai autorisé",
                    ex,
                    timeoutSeconds * 1000L
            );

        } catch (HttpClientErrorException ex) {
            logger.error("IA API client error ({}): {}",
                    ex.getStatusCode(), ex.getMessage());

            throw new IAServiceException(
                    "Erreur de requête vers le service IA: " + ex.getMessage(),
                    "IA_CLIENT_ERROR",
                    ex.getStatusCode().value()
            );

        } catch (HttpServerErrorException ex) {
            logger.error("IA API server error ({}): {}",
                    ex.getStatusCode(), ex.getMessage());

            throw new IAServiceException(
                    "Le service IA rencontre des problèmes techniques",
                    "IA_SERVER_ERROR",
                    ex.getStatusCode().value()
            );

        } catch (Exception ex) {
            logger.error("Unexpected error during CV analysis", ex);

            throw new IAServiceException(
                    "Erreur inattendue lors de l'analyse: " + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Call real Python IA API with timeout protection
     */
    private AIAnalysisResult callRealIAApi(String cvText, String jobDescription) {
        logger.debug("Calling real IA API at: {}", iaApiUrl);

        try {
            // Prepare request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Prepare request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("cv_text", cvText);
            requestBody.put("job_description", jobDescription);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            logger.debug("Sending request to IA API with timeout: {}s", timeoutSeconds);
            long startTime = System.currentTimeMillis();

            // Make request with timeout (configured in RestTemplate)
            ResponseEntity<AIAnalysisResult> response = restTemplate.exchange(
                    iaApiUrl,
                    HttpMethod.POST,
                    entity,
                    AIAnalysisResult.class
            );

            long duration = System.currentTimeMillis() - startTime;
            logger.info("IA API responded in {}ms with status: {}",
                    duration, response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IAServiceException(
                        "IA API returned non-success status: " + response.getStatusCode(),
                        "IA_NON_SUCCESS_STATUS",
                        response.getStatusCode().value()
                );
            }

            AIAnalysisResult result = response.getBody();

            if (result == null) {
                throw new IAServiceException(
                        "IA API returned empty response",
                        "IA_EMPTY_RESPONSE"
                );
            }

            return result;

        } catch (ResourceAccessException ex) {
            // This includes timeout exceptions
            logger.error("Resource access error when calling IA API", ex);
            throw ex; // Re-throw to be caught by main try-catch

        } catch (Exception ex) {
            logger.error("Unexpected error when calling IA API", ex);
            throw new IAServiceException(
                    "Erreur lors de l'appel au service IA: " + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Generate MOCK analysis for development/testing
     *
     * Simulates realistic IA response with controlled delay
     */
    private AIAnalysisResult generateMockAnalysis(String cvText, String jobDescription) {
        logger.info("Generating MOCK IA analysis (development mode)");

        // Simulate network/processing delay (500ms - well under timeout)
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Mock delay interrupted");
        }

        AIAnalysisResult result = new AIAnalysisResult();

        // Generate realistic score (60-95%)
        result.setScore(60.0 + Math.random() * 35);

        // Mock data - realistic but fake
        result.setSkillsFound(Arrays.asList(
                "Java", "Spring Boot", "MySQL", "Git", "RESTful APIs"
        ));

        result.setMissingSkills(Arrays.asList(
                "Docker", "Kubernetes", "Microservices", "AWS"
        ));

        result.setStrengths(Arrays.asList(
                "Expérience solide en développement backend Java",
                "Bonne maîtrise des frameworks Spring",
                "Compétences en bases de données relationnelles"
        ));

        result.setImprovements(Arrays.asList(
                "Acquérir des compétences en conteneurisation",
                "Se former sur les architectures cloud"
        ));

        // Recommendations
        result.setRecommendations(Arrays.asList(
                new AIAnalysisResult.AIRecommendation(
                        "COMPETENCE_A_ACQUERIR",
                        "Suivre une formation Docker et Kubernetes pour répondre aux exigences du poste",
                        1,
                        "DEVOPS"
                ),
                new AIAnalysisResult.AIRecommendation(
                        "FORMATION_SUGGEREE",
                        "Certification AWS Solutions Architect pour renforcer le profil",
                        2,
                        "CLOUD"
                ),
                new AIAnalysisResult.AIRecommendation(
                        "AMELIORATION_CV",
                        "Mettre en avant les projets Spring Boot dans la section expérience",
                        2,
                        "CV"
                ),
                new AIAnalysisResult.AIRecommendation(
                        "CONSEIL_ENTRETIEN",
                        "Préparer des exemples concrets de résolution de problèmes en backend",
                        3,
                        "ENTRETIEN"
                )
        ));

        logger.info("MOCK analysis completed - Score: {:.2f}%", result.getScore());
        return result;
    }

    /**
     * Test IA API availability
     *
     * @return true if IA API is reachable, false otherwise
     */
    public boolean testIAApiConnection() {
        if (mockEnabled) {
            logger.info("MOCK mode enabled - skipping real API test");
            return true;
        }

        try {
            logger.info("Testing IA API connection at: {}", iaApiUrl);

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    iaApiUrl.replace("/analyze", "/health"),
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            boolean isAvailable = response.getStatusCode().is2xxSuccessful();
            logger.info("IA API connection test: {}", isAvailable ? "SUCCESS" : "FAILED");

            return isAvailable;

        } catch (Exception ex) {
            logger.error("IA API connection test failed", ex);
            return false;
        }
    }
}