package com.ahmed.pfa.cvplatform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration
 *
 * Generates interactive API documentation accessible at:
 * http://localhost:8080/swagger-ui.html
 *
 * Features:
 * - All endpoints documented
 * - JWT authentication support
 * - Try-it-out functionality
 * - Request/Response examples
 *
 * @author Ahmed
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI cvPlatformOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(apiServers())
                .components(securityComponents())
                .addSecurityItem(securityRequirement());
    }

    /**
     * API Information and Metadata
     */
    private Info apiInfo() {
        return new Info()
                .title("CV Platform API")
                .description("""
                    ## Plateforme d'Analyse Intelligente de CV
                    
                    API REST pour la gestion et l'analyse de CV avec intelligence artificielle.
                    
                    ### Fonctionnalités principales:
                    - **Authentification JWT** avec refresh tokens (durée: 15 min / 7 jours)
                    - **Gestion de CV** (upload PDF/DOCX, extraction texte)
                    - **Analyse IA** des CV par rapport aux offres d'emploi
                    - **Offres privées** ciblées pour les étudiants
                    - **Rate limiting** (5 tentatives/minute sur login)
                    
                    ### Sécurité:
                    - Rate limiting anti-brute force
                    - Timeout protection sur services IA (30s)
                    - Role-based access control (ADMIN, ETUDIANT)
                    
                    ### Comment utiliser:
                    1. **Register** un nouveau compte (`POST /api/auth/register`)
                    2. **Login** pour obtenir les tokens (`POST /api/auth/login`)
                    3. **Autoriser** en cliquant sur le cadenas 🔒 et copier l'access token
                    4. **Tester** les endpoints protégés
                    
                    ---
                    
                    **Note:** Les endpoints publics (auth) ne nécessitent pas d'authentification.
                    """)
                .version("0.3.0")
                .contact(new Contact()
                        .name("Ahmed - Projet PFA")
                        .email("ahmed@cvplatform.com")
                        .url("https://github.com/ahmed/cvplatform"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * API Servers
     */
    private List<Server> apiServers() {
        Server devServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Development Server");

        Server prodServer = new Server()
                .url("https://api.cvplatform.com")
                .description("Production Server (example)");

        return List.of(devServer, prodServer);
    }

    /**
     * Security Components - JWT Bearer Token
     */
    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .description("""
                            ### JWT Authentication
                            
                            Pour utiliser les endpoints protégés:
                            
                            1. Obtenez un token via `POST /api/auth/login`
                            2. Copiez l'**accessToken** de la réponse
                            3. Cliquez sur le bouton **Authorize** 🔒 ci-dessus
                            4. Collez le token (PAS besoin d'ajouter "Bearer ")
                            5. Cliquez sur **Authorize**
                            
                            Le token expire après **15 minutes**.
                            Utilisez le **refreshToken** via `/api/auth/refresh` pour renouveler.
                            """));
    }

    /**
     * Security Requirement - Apply JWT to all endpoints
     */
    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList("bearer-jwt");
    }
}