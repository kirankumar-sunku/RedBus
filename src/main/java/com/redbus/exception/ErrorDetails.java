package com.redbus.exception;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorDetails {
    private LocalDateTime timestamp;
    private String message;
    private String details;
    private String errorCode;

    public ErrorDetails(String message, String details, String errorCode) {
        this.timestamp = LocalDateTime.now();
        this.message = message;
        this.details = details;
        this.errorCode = errorCode;
    }
}
