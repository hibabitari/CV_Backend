package com.ahmed.pfa.cvplatform.integration;

import com.ahmed.pfa.cvplatform.dto.LoginRequest;
import com.ahmed.pfa.cvplatform.dto.RegisterRequest;
import com.ahmed.pfa.cvplatform.security.LoginRateLimitFilter;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LoginRateLimitFilter loginRateLimitFilter;

    /** Champs requis pour l'inscription ETUDIANT (table etudiant). */
    private static void fillEtudiantScolarite(RegisterRequest request) {
        request.setNiveauEtude("Licence");
        request.setDomaineEtude("Informatique");
        request.setUniversite("Université Test");
    }

    @BeforeEach
    void setUp() {
        // Réinitialiser le rate limiter avant chaque test
        // pour éviter les interférences entre tests
        if (loginRateLimitFilter != null) {
            loginRateLimitFilter.clearCache();
        }
    }

    @Test
    void testRegisterUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setNom("Test");
        request.setPrenom("Integration");
        request.setEmail("integration.test@example.com");
        request.setMotDePasse("SecurePass123!");
        request.setRole("ETUDIANT");
        fillEtudiantScolarite(request);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("integration.test@example.com"));
        // ✅ SUPPRIMÉ: .andExpect(jsonPath("$.role").value("ETUDIANT"))
        // AuthResponse ne contient pas de champ "role"
    }

    @Test
    void testLoginSuccess() throws Exception {
        // Créer un utilisateur d'abord
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setNom("Login");
        registerRequest.setPrenom("Test");
        registerRequest.setEmail("login.test@example.com");
        registerRequest.setMotDePasse("SecurePass123!");
        registerRequest.setRole("ETUDIANT");
        fillEtudiantScolarite(registerRequest);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)));

        // Tester le login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login.test@example.com");
        loginRequest.setMotDePasse("SecurePass123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.email").value("login.test@example.com"));
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setMotDePasse("WrongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRefreshToken() throws Exception {
        // Créer et login un utilisateur
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setNom("Refresh");
        registerRequest.setPrenom("Token");
        registerRequest.setEmail("refresh.test@example.com");
        registerRequest.setMotDePasse("SecurePass123!");
        registerRequest.setRole("ETUDIANT");
        fillEtudiantScolarite(registerRequest);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("refresh.test@example.com");
        loginRequest.setMotDePasse("SecurePass123!");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // ✅ CORRIGÉ: Utiliser JsonNode pour extraire refreshToken
        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String refreshToken = loginJson.get("refreshToken").asText();

        // Tester le refresh token
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void testLogout() throws Exception {
        // Créer et login un utilisateur
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setNom("Logout");
        registerRequest.setPrenom("Test");
        registerRequest.setEmail("logout.test@example.com");
        registerRequest.setMotDePasse("SecurePass123!");
        registerRequest.setRole("ETUDIANT");
        fillEtudiantScolarite(registerRequest);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("logout.test@example.com");
        loginRequest.setMotDePasse("SecurePass123!");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // ✅ CORRIGÉ: Utiliser JsonNode pour extraire refreshToken
        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String refreshToken = loginJson.get("refreshToken").asText();

        // Tester le logout
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testLoginRateLimiting() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("ratelimit.test@example.com");
        request.setMotDePasse("WrongPassword123!");

        // Faire 6 tentatives de login échouées
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)));
        }

        // La 7ème tentative devrait être bloquée (429 Too Many Requests)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void testChangePassword() throws Exception {
        // Créer et login un utilisateur
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setNom("Password");
        registerRequest.setPrenom("Change");
        registerRequest.setEmail("password.test@example.com");
        registerRequest.setMotDePasse("OldPass123!");
        registerRequest.setRole("ETUDIANT");
        fillEtudiantScolarite(registerRequest);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("password.test@example.com");
        loginRequest.setMotDePasse("OldPass123!");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // ✅ CORRIGÉ: Utiliser JsonNode pour extraire accessToken
        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String accessToken = loginJson.get("accessToken").asText();

        // Changer le mot de passe
        String changePasswordRequest = "{\"ancienMotDePasse\":\"OldPass123!\",\"nouveauMotDePasse\":\"NewPass123!\",\"confirmationMotDePasse\":\"NewPass123!\"}";

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordRequest))
                .andExpect(status().isOk());

        // Vérifier que l'ancien mot de passe ne fonctionne plus
        LoginRequest oldPasswordLogin = new LoginRequest();
        oldPasswordLogin.setEmail("password.test@example.com");
        oldPasswordLogin.setMotDePasse("OldPass123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(oldPasswordLogin)))
                .andExpect(status().isUnauthorized());

        // ✅ AJOUT: Réinitialiser le rate limiter avant de tester le nouveau password
        // pour éviter le blocage 429 après 2 tentatives de login
        loginRateLimitFilter.clearCache();

        // Vérifier que le nouveau mot de passe fonctionne
        LoginRequest newPasswordLogin = new LoginRequest();
        newPasswordLogin.setEmail("password.test@example.com");
        newPasswordLogin.setMotDePasse("NewPass123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(newPasswordLogin)))
                .andExpect(status().isOk());
    }

    @Test
    void testForgotPasswordVerifyEmail_successAndTrailingSlash() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setNom("Forgot");
        registerRequest.setPrenom("Verify");
        registerRequest.setEmail("forgot.verify@example.com");
        registerRequest.setMotDePasse("SecurePass123!");
        registerRequest.setRole("ETUDIANT");
        fillEtudiantScolarite(registerRequest);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(registerRequest)));

        mockMvc.perform(post("/api/auth/forgot-password/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"forgot.verify@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(post("/api/auth/forgot-password/verify-email/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"forgot.verify@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testForgotPasswordVerifyEmail_getReturns405() throws Exception {
        mockMvc.perform(get("/api/auth/forgot-password/verify-email"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testForgotPasswordVerifyEmail_unknownEmail404() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"absent@example.com\"}"))
                .andExpect(status().isNotFound());
    }
}