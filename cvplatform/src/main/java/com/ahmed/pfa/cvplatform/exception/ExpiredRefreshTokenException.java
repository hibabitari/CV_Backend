package com.ahmed.pfa.cvplatform.exception;

/**
 * Exception levée quand un refresh token a expiré
 *
 * Cas d'usage:
 * - Token expiré (après 7 jours)
 * - User doit se reconnecter
 *
 * @author Ahmed
 */
public class ExpiredRefreshTokenException extends RuntimeException {

    public ExpiredRefreshTokenException(String message) {
        super(message);
    }

    public ExpiredRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}