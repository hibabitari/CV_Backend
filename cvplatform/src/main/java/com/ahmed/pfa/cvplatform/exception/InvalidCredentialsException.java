package com.ahmed.pfa.cvplatform.exception;

/**
 * Exception levée pour credentials invalides
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email ou mot de passe incorrect");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}