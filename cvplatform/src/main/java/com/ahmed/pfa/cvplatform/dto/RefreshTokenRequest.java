package com.ahmed.pfa.cvplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour la requête de renouvellement de token
 *
 * Utilisé par: POST /api/auth/refresh
 *
 * @author Ahmed
 */
@Data
public class RefreshTokenRequest {

    /**
     * Refresh token à renouveler
     * Doit être un JWT valide et non révoqué
     */
    @NotBlank(message = "Le refresh token est requis")
    private String refreshToken;
}