package com.ahmed.pfa.cvplatform.exception;

/**
 * Exception générique pour ressource non trouvée
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " avec l'ID " + id + " non trouvé");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}