package com.ahmetsenel.commonlib.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponse {
    private boolean success;
    private String message;
    private int status;
    private int errorCode;
    private LocalDateTime timestamp;

    public ErrorResponse(String message, int status, int errorCode) {
        this.success = false;
        this.message = message;
        this.status = status;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }
}
