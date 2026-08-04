package com.ahmetsenel.chatservice.dto;

import lombok.Data;

@Data
public class TypingRequest {
    private String type;
    private Long id;
}