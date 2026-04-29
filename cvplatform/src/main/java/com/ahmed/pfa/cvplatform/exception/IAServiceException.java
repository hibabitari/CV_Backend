package com.ahmed.pfa.cvplatform.exception;

/**
 * Exception thrown when IA service encounters an error
 *
 * This includes:
 * - API unavailable
 * - Network errors
 * - Invalid responses
 * - Service errors
 *
 * @author Ahmed
 */
public class IAServiceException extends RuntimeException {

    private final String errorCode;
    private final Integer httpStatus;

    public IAServiceException(String message) {
        super(message);
        this.errorCode = "IA_SERVICE_ERROR";
        this.httpStatus = null;
    }

    public IAServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "IA_SERVICE_ERROR";
        this.httpStatus = null;
    }

    public IAServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = null;
    }

    public IAServiceException(String message, String errorCode, Integer httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public IAServiceException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = null;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }
}