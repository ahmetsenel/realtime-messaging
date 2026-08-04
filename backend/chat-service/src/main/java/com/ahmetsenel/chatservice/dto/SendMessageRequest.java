package com.ahmetsenel.chatservice.dto;

import com.ahmetsenel.chatservice.entity.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public  class SendMessageRequest {
    private ConversationType type;
    private String content;
    private Long receiverId;
    private String receiverUsername;
    private Long replyToId;
    private Long groupId;
}