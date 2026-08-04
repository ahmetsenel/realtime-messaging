package com.ahmetsenel.commonlib.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final MessageType messageType;

    public BusinessException(MessageType messageType) {
        super(messageType.getMessage());
        this.messageType = messageType;
    }
}