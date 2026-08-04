package com.ahmetsenel.commonlib.response;

import com.ahmetsenel.commonlib.exception.BusinessException;
import com.ahmetsenel.commonlib.exception.MessageType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        MessageType type = ex.getMessageType();

        ErrorResponse error = new ErrorResponse(
                type.getMessage(),
                type.getStatus(),
                type.getCode()
        );
        return ResponseEntity.status(type.getStatus()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        ex.printStackTrace();

        MessageType type = MessageType.INTERNAL_SERVER_ERROR;
        ErrorResponse error = new ErrorResponse(
                type.getMessage(),
                type.getStatus(),
                type.getCode()
        );
        return ResponseEntity.status(type.getStatus()).body(error);
    }
}
