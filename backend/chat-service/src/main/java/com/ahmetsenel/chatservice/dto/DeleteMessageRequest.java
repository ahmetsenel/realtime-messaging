package com.ahmetsenel.chatservice.dto;

import lombok.Data;

@Data
public class DeleteMessageRequest {
    private String type;
    private Long id;
    private Long messageId;
}
