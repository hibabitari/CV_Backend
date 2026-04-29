package com.ahmed.pfa.cvplatform.service;

import com.ahmed.pfa.cvplatform.exception.ExpiredRefreshTokenException;
import com.ahmed.pfa.cvplatform.exception.InvalidRefreshTokenException;
import com.ahmed.pfa.cvplatform.model.RefreshToken;
import com.ahmed.pfa.cvplatform.model.Utilisateur;
import com.ahmed.pfa.cvplatform.model.Etudiant;
import com.ahmed.pfa.cvplatform.repository.RefreshTokenRepository;
import com.ahmed.pfa.cvplatform.repository.UtilisateurRepository;
import com.ahmed.pfa.cvplatform.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour RefreshTokenService
 *
 * Couvre:
 * - Création refresh token
 * - Validation (succès + échecs)
 * - Révocation (individuelle + globale)
 *
 * @author Ahmed
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private Utilisateur testUser;
    private RefreshToken testRefreshToken;
    private String testTokenString;

    @BeforeEach
    void setUp() {
        // Configurer refreshTokenExpiration (7 jours en ms)
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 604800000L);

        // Créer utilisateur test
        testUser = new Etudiant();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setRole("ETUDIANT");

        // Créer token test
        testTokenString = "eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCJ9.test";

        testRefreshToken = RefreshToken.builder()
                .id(1L)
                .token(testTokenString)
                .utilisateur(testUser)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
    }

    // ========================================
    // TESTS: createRefreshToken()
    // ========================================

    @Test
    @DisplayName("createRefreshToken() - devrait créer et sauvegarder un refresh token valide")
    void createRefreshToken_ShouldCreateAndSaveToken() {
        // Given
        when(jwtUtil.generateRefreshToken(anyString(), anyLong()))
                .thenReturn(testTokenString);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(testRefreshToken);

        // When
        RefreshToken result = refreshTokenService.createRefreshToken(testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(testTokenString);
        assertThat(result.getUtilisateur()).isEqualTo(testUser);
        assertThat(result.getRevoked()).isFalse();

        verify(jwtUtil).generateRefreshToken(testUser.getEmail(), testUser.getId());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("createRefreshToken() - devrait définir la date d'expiration à +7 jours")
    void createRefreshToken_ShouldSetCorrectExpiration() {
        // Given
        when(jwtUtil.generateRefreshToken(anyString(), anyLong()))
                .thenReturn(testTokenString);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RefreshToken result = refreshTokenService.createRefreshToken(testUser);

        // Then
        assertThat(result.getExpiresAt())
                .isAfter(LocalDateTime.now().plusDays(6))
                .isBefore(LocalDateTime.now().plusDays(8));
    }

    // ========================================
    // TESTS: validateRefreshToken()
    // ========================================

    @Test
    @DisplayName("validateRefreshToken() - devrait valider un token valide")
    void validateRefreshToken_WithValidToken_ShouldReturnToken() {
        // Given
        when(refreshTokenRepository.findByToken(testTokenString))
                .thenReturn(Optional.of(testRefreshToken));
        when(jwtUtil.validateRefreshToken(testTokenString, testUser.getEmail()))
                .thenReturn(true);

        // When
        RefreshToken result = refreshTokenService.validateRefreshToken(testTokenString);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testRefreshToken);
        verify(refreshTokenRepository).findByToken(testTokenString);
        verify(jwtUtil).validateRefreshToken(testTokenString, testUser.getEmail());
    }

    @Test
    @DisplayName("validateRefreshToken() - devrait lancer InvalidRefreshTokenException si token non trouvé")
    void validateRefreshToken_WithNonExistentToken_ShouldThrowException() {
        // Given
        when(refreshTokenRepository.findByToken(anyString()))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("invalid-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("invalide");

        verify(refreshTokenRepository).findByToken("invalid-token");
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("validateRefreshToken() - devrait lancer InvalidRefreshTokenException si token révoqué")
    void validateRefreshToken_WithRevokedToken_ShouldThrowException() {
        // Given
        testRefreshToken.setRevoked(true);
        testRefreshToken.setRevokedAt(LocalDateTime.now());

        when(refreshTokenRepository.findByToken(testTokenString))
                .thenReturn(Optional.of(testRefreshToken));

        // When / Then
        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(testTokenString))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("révoqué");
    }

    @Test
    @DisplayName("validateRefreshToken() - devrait lancer ExpiredRefreshTokenException si token expiré")
    void validateRefreshToken_WithExpiredToken_ShouldThrowException() {
        // Given
        testRefreshToken.setExpiresAt(LocalDateTime.now().minusDays(1)); // Expiré hier

        when(refreshTokenRepository.findByToken(testTokenString))
                .thenReturn(Optional.of(testRefreshToken));

        // When / Then
        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(testTokenString))
                .isInstanceOf(ExpiredRefreshTokenException.class)
                .hasMessageContaining("expiré");
    }

    @Test
    @DisplayName("validateRefreshToken() - devrait lancer InvalidRefreshTokenException si signature JWT invalide")
    void validateRefreshToken_WithInvalidJwtSignature_ShouldThrowException() {
        // Given
        when(refreshTokenRepository.findByToken(testTokenString))
                .thenReturn(Optional.of(testRefreshToken));
        when(jwtUtil.validateRefreshToken(testTokenString, testUser.getEmail()))
                .thenReturn(false); // Signature invalide

        // When / Then
        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken(testTokenString))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("Signature");
    }

    // ========================================
    // TESTS: revokeRefreshToken()
    // ========================================

    @Test
    @DisplayName("revokeRefreshToken() - devrait révoquer un token existant")
    void revokeRefreshToken_WithValidToken_ShouldRevokeToken() {
        // Given
        when(refreshTokenRepository.findByToken(testTokenString))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(testRefreshToken);

        // When
        refreshTokenService.revokeRefreshToken(testTokenString);

        // Then
        assertThat(testRefreshToken.getRevoked()).isTrue();
        assertThat(testRefreshToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(testRefreshToken);
    }

    @Test
    @DisplayName("revokeRefreshToken() - devrait lancer InvalidRefreshTokenException si token non trouvé")
    void revokeRefreshToken_WithNonExistentToken_ShouldThrowException() {
        // Given
        when(refreshTokenRepository.findByToken(anyString()))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> refreshTokenService.revokeRefreshToken("non-existent"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    // ========================================
    // TESTS: revokeAllUserTokens()
    // ========================================

    @Test
    @DisplayName("revokeAllUserTokens() - devrait révoquer tous les tokens d'un utilisateur")
    void revokeAllUserTokens_ShouldRevokeAllTokens() {
        // Given
        Long userId = 1L;
        when(utilisateurRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.revokeAllByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(3); // 3 tokens révoqués

        // When
        int revokedCount = refreshTokenService.revokeAllUserTokens(userId);

        // Then
        assertThat(revokedCount).isEqualTo(3);
        verify(utilisateurRepository).findById(userId);
        verify(refreshTokenRepository).revokeAllByUser(eq(testUser), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("revokeAllUserTokens() - devrait retourner 0 si aucun token à révoquer")
    void revokeAllUserTokens_WithNoTokens_ShouldReturnZero() {
        // Given
        Long userId = 1L;
        when(utilisateurRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.revokeAllByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(0);

        // When
        int revokedCount = refreshTokenService.revokeAllUserTokens(userId);

        // Then
        assertThat(revokedCount).isEqualTo(0);
    }
}
