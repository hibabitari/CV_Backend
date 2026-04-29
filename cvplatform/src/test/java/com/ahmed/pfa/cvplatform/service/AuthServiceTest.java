package com.ahmed.pfa.cvplatform.service;

import com.ahmed.pfa.cvplatform.dto.*;
import com.ahmed.pfa.cvplatform.exception.*;
import com.ahmed.pfa.cvplatform.model.Etudiant;
import com.ahmed.pfa.cvplatform.model.RefreshToken;
import com.ahmed.pfa.cvplatform.model.Utilisateur;
import com.ahmed.pfa.cvplatform.repository.AdministrateurRepository;
import com.ahmed.pfa.cvplatform.repository.EtudiantRepository;
import com.ahmed.pfa.cvplatform.repository.UtilisateurRepository;
import com.ahmed.pfa.cvplatform.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AuthService
 *
 * Couvre:
 * - Login avec refresh token
 * - Refresh access token
 * - Logout
 * - Change password (avec révocation tokens)
 *
 * @author Ahmed
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private EtudiantRepository etudiantRepository;

    @Mock
    private AdministrateurRepository administrateurRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Utilisateur testUser;
    private LoginRequest loginRequest;
    private RefreshToken testRefreshToken;
    private String testAccessToken;
    private String testRefreshTokenString;

    @BeforeEach
    void setUp() {
        // Créer utilisateur test
        testUser = new Etudiant();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setMotDePasse("$2a$10$hashedPassword");
        testUser.setRole("ETUDIANT");

        // Créer login request
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setMotDePasse("PlainPassword123!");

        // Créer tokens
        testAccessToken = "eyJhbGciOiJIUzI1NiJ9.access.token";
        testRefreshTokenString = "eyJhbGciOiJIUzI1NiJ9.refresh.token";

        testRefreshToken = RefreshToken.builder()
                .id(1L)
                .token(testRefreshTokenString)
                .utilisateur(testUser)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
    }

    // ========================================
    // TESTS: login()
    // ========================================

    @Test
    @DisplayName("login() - devrait retourner access + refresh tokens pour credentials valides")
    void login_WithValidCredentials_ShouldReturnBothTokens() {
        // Given
        when(utilisateurRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(testUser);
        when(passwordEncoder.matches(loginRequest.getMotDePasse(), testUser.getMotDePasse()))
                .thenReturn(true);
        when(jwtUtil.generateAccessToken(anyString(), anyLong(), anyString(),anyString(), anyString()))
                .thenReturn(testAccessToken);
        when(refreshTokenService.createRefreshToken(testUser))
                .thenReturn(testRefreshToken);
        when(jwtUtil.getAccessTokenExpirationInSeconds())
                .thenReturn(900L);

        // When
        AuthTokenResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(testAccessToken);
        assertThat(response.getRefreshToken()).isEqualTo(testRefreshTokenString);
        assertThat(response.getUserId()).isEqualTo(testUser.getId());
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(response.getRole()).isEqualTo(testUser.getRole());
        assertThat(response.getExpiresIn()).isEqualTo(900L);
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        verify(utilisateurRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getMotDePasse(), testUser.getMotDePasse());
        verify(jwtUtil).generateAccessToken(testUser.getEmail(), testUser.getId(), testUser.getRole(), testUser.getNom(), testUser.getPrenom());
        verify(refreshTokenService).createRefreshToken(testUser);
    }

    @Test
    @DisplayName("login() - devrait lancer InvalidCredentialsException si utilisateur non trouvé")
    void login_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(utilisateurRepository.findByEmail(anyString()))
                .thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(utilisateurRepository).findByEmail(loginRequest.getEmail());
        verifyNoInteractions(passwordEncoder, jwtUtil, refreshTokenService);
    }

    @Test
    @DisplayName("login() - devrait lancer InvalidCredentialsException si mot de passe incorrect")
    void login_WithWrongPassword_ShouldThrowException() {
        // Given
        when(utilisateurRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(testUser);
        when(passwordEncoder.matches(loginRequest.getMotDePasse(), testUser.getMotDePasse()))
                .thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder).matches(loginRequest.getMotDePasse(), testUser.getMotDePasse());
        verifyNoInteractions(jwtUtil, refreshTokenService);
    }

    // ========================================
    // TESTS: refresh()
    // ========================================

    @Test
    @DisplayName("refresh() - devrait retourner nouveau access token pour refresh token valide")
    void refresh_WithValidRefreshToken_ShouldReturnNewAccessToken() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(testRefreshTokenString);

        when(refreshTokenService.validateRefreshToken(testRefreshTokenString))
                .thenReturn(testRefreshToken);
        when(jwtUtil.generateAccessToken(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn("newAccessToken");
        when(jwtUtil.getAccessTokenExpirationInSeconds())
                .thenReturn(900L);

        // When
        AuthTokenResponse response = authService.refresh(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(response.getRefreshToken()).isEqualTo(testRefreshTokenString); // Même refresh token
        assertThat(response.getMessage()).contains("renouvelé");

        verify(refreshTokenService).validateRefreshToken(testRefreshTokenString);
        verify(jwtUtil).generateAccessToken(testUser.getEmail(), testUser.getId(), testUser.getRole(), testUser.getNom(), testUser.getPrenom());
    }

    @Test
    @DisplayName("refresh() - devrait propager InvalidRefreshTokenException du service")
    void refresh_WithInvalidToken_ShouldThrowException() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-token");

        when(refreshTokenService.validateRefreshToken("invalid-token"))
                .thenThrow(new InvalidRefreshTokenException("Refresh token invalide"));

        // When / Then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("invalide");

        verify(refreshTokenService).validateRefreshToken("invalid-token");
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("refresh() - devrait propager ExpiredRefreshTokenException du service")
    void refresh_WithExpiredToken_ShouldThrowException() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(testRefreshTokenString);

        when(refreshTokenService.validateRefreshToken(testRefreshTokenString))
                .thenThrow(new ExpiredRefreshTokenException("Refresh token expiré"));

        // When / Then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(ExpiredRefreshTokenException.class)
                .hasMessageContaining("expiré");
    }

    // ========================================
    // TESTS: logout()
    // ========================================

    @Test
    @DisplayName("logout() - devrait révoquer le refresh token")
    void logout_ShouldRevokeRefreshToken() {
        // Given
        doNothing().when(refreshTokenService).revokeRefreshToken(testRefreshTokenString);

        // When
        authService.logout(testRefreshTokenString);

        // Then
        verify(refreshTokenService).revokeRefreshToken(testRefreshTokenString);
    }

    @Test
    @DisplayName("logout() - devrait propager InvalidRefreshTokenException si token non trouvé")
    void logout_WithNonExistentToken_ShouldThrowException() {
        // Given
        doThrow(new InvalidRefreshTokenException("Token non trouvé"))
                .when(refreshTokenService).revokeRefreshToken(anyString());

        // When / Then
        assertThatThrownBy(() -> authService.logout("invalid-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    // ========================================
    // TESTS: changePassword()
    // ========================================

    @Test
    @DisplayName("changePassword() - devrait changer password et révoquer tous les tokens")
    void changePassword_WithValidData_ShouldChangePasswordAndRevokeTokens() {
        // Given
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setAncienMotDePasse("OldPassword123!");
        request.setNouveauMotDePasse("NewPassword456!");
        request.setConfirmationMotDePasse("NewPassword456!");

        when(utilisateurRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String plain = invocation.getArgument(0);
                    String hashed = invocation.getArgument(1);
                    // OldPassword123! matches $2a$10$hashedPassword
                    if ("OldPassword123!".equals(plain) && "$2a$10$hashedPassword".equals(hashed)) {
                        return true;
                    }
                    // NewPassword456! does NOT match $2a$10$hashedPassword
                    if ("NewPassword456!".equals(plain) && "$2a$10$hashedPassword".equals(hashed)) {
                        return false;
                    }
                    return false;
                });
        when(passwordEncoder.encode(request.getNouveauMotDePasse()))
                .thenReturn("$2a$10$newHashedPassword");
        when(utilisateurRepository.save(any(Utilisateur.class)))
                .thenReturn(testUser);
        when(refreshTokenService.revokeAllUserTokens(userId))
                .thenReturn(3); // 3 tokens révoqués

        // When
        authService.changePassword(userId, request);

        // Then
        verify(utilisateurRepository).findById(userId);
        verify(passwordEncoder, atLeastOnce()).matches(anyString(), anyString());
        verify(passwordEncoder).encode(request.getNouveauMotDePasse());
        verify(utilisateurRepository).save(any(Utilisateur.class));
        verify(refreshTokenService).revokeAllUserTokens(userId);
    }

    @Test
    @DisplayName("changePassword() - devrait lancer PasswordMismatchException si confirmation différente")
    void changePassword_WithMismatchedConfirmation_ShouldThrowException() {
        // Given
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setAncienMotDePasse("OldPassword123!");
        request.setNouveauMotDePasse("NewPassword456!");
        request.setConfirmationMotDePasse("DifferentPassword789!");

        // When / Then
        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(PasswordMismatchException.class)
                .hasMessageContaining("correspondent pas");

        verifyNoInteractions(utilisateurRepository, passwordEncoder, refreshTokenService);
    }

    @Test
    @DisplayName("changePassword() - devrait lancer InvalidCredentialsException si ancien password incorrect")
    void changePassword_WithWrongOldPassword_ShouldThrowException() {
        // Given
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setAncienMotDePasse("WrongOldPassword!");
        request.setNouveauMotDePasse("NewPassword456!");
        request.setConfirmationMotDePasse("NewPassword456!");

        when(utilisateurRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getAncienMotDePasse(), testUser.getMotDePasse()))
                .thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("ancien mot de passe");

        verify(utilisateurRepository).findById(userId);
        verify(passwordEncoder).matches(request.getAncienMotDePasse(), testUser.getMotDePasse());
        verify(refreshTokenService, never()).revokeAllUserTokens(anyLong());
    }

    @Test
    @DisplayName("changePassword() - devrait lancer SamePasswordException si nouveau == ancien")
    void changePassword_WithSamePassword_ShouldThrowException() {
        // Given
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setAncienMotDePasse("SamePassword123!");
        request.setNouveauMotDePasse("SamePassword123!");
        request.setConfirmationMotDePasse("SamePassword123!");

        when(utilisateurRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getAncienMotDePasse(), testUser.getMotDePasse()))
                .thenReturn(true);
        when(passwordEncoder.matches(request.getNouveauMotDePasse(), testUser.getMotDePasse()))
                .thenReturn(true); // Nouveau == ancien

        // When / Then
        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(SamePasswordException.class)
                .hasMessageContaining("différent");

        verify(refreshTokenService, never()).revokeAllUserTokens(anyLong());
    }
}