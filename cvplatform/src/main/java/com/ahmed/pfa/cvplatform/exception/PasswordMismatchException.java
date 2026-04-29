package com.ahmed.pfa.cvplatform.exception;

/**
 * Exception levée quand le nouveau mot de passe et sa confirmation ne correspondent pas
 *
 * @author Ahmed
 */
public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException(String message) {
        super(message);
    }
}