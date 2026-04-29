package com.ahmed.pfa.cvplatform.exception;

/**
 * Exception levée quand l'email existe déjà
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("L'email " + email + " est déjà utilisé");
    }
}