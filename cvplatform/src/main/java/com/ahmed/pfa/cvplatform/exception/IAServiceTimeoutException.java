package com.ahmed.pfa.cvplatform.exception;

/**
 * Exception thrown when IA service request times out
 *
 * This occurs when the external IA API takes too long to respond
 * and exceeds the configured timeout threshold.
 *
 * @author Ahmed
 */
public class IAServiceTimeoutException extends IAServiceException {

    private final long timeoutMillis;

    public IAServiceTimeoutException(String message, long timeoutMillis) {
        super(message, "IA_SERVICE_TIMEOUT");
        this.timeoutMillis = timeoutMillis;
    }

    public IAServiceTimeoutException(String message, Throwable cause, long timeoutMillis) {
        super(message, cause, "IA_SERVICE_TIMEOUT");
        this.timeoutMillis = timeoutMillis;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public long getTimeoutSeconds() {
        return timeoutMillis / 1000;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " (timeout: " + getTimeoutSeconds() + "s)";
    }
}