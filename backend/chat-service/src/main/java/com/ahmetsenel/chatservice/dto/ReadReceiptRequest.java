package com.ahmetsenel.chatservice.dto;

import lombok.Data;

@Data
public class ReadReceiptRequest {
    private String type;
    private Long id;
}
