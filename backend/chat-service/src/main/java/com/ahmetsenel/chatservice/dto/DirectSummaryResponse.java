package com.ahmetsenel.chatservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class DirectSummaryResponse {
    private String key;
    private String type;
    private Long id;
    private String name;
    private String lastMessage;
    private LocalDateTime lastTime;
    private String lastSender;
    private long unread;
}
