package com.ahmed.pfa.cvplatform.exception;

/**
 * Exception levée quand un refresh token est invalide
 *
 * Cas d'usage:
 * - Token non trouvé en base
 * - Token révoqué
 * - Token malformé
 * - Signature invalide
 *
 * @author Ahmed
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    public InvalidRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}