package com.redbus.exception;

public class RedBusException extends RuntimeException {
    // 1. Private fields (hidden from outside classes)
    private final String errorCode;
    private final String errorMessage;

    // 2. Constructor to initialize the values
    public RedBusException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    // 3. EXACT GETTER FOR errorCode
    public String getErrorCode() {
        return this.errorCode;
    }

    // 4. EXACT GETTER FOR errorMessage
    public String getErrorMessage() {
        return this.errorMessage;
    }

}
