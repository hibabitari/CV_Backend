package com.ahmed.pfa.cvplatform.exception;

/**
 * Exception levée quand un utilisateur n'est pas trouvé
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Long userId) {
        super("Utilisateur avec l'ID " + userId + " non trouvé");
    }
}