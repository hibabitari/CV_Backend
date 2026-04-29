package com.ahmed.pfa.cvplatform.exception;

/**
 * Exception levée quand le nouveau mot de passe est identique à l'ancien
 *
 * @author Ahmed
 */
public class SamePasswordException extends RuntimeException {

    public SamePasswordException(String message) {
        super(message);
    }
}