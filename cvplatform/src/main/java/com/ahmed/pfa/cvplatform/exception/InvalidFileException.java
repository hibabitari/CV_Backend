package com.ahmed.pfa.cvplatform.exception;

/**
 * Exception levée pour les fichiers invalides
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}