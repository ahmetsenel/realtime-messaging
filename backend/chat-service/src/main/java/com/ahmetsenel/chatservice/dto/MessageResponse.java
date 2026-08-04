package com.ahmetsenel.chatservice.dto;

import com.ahmetsenel.chatservice.entity.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private ConversationType type;
    private Long senderId;
    private String senderUsername;
    private Long receiverId;
    private String receiverUsername;
    private Long groupId;
    private String content;
    private boolean read;
    private boolean delivered;
    private boolean deleted;
    private Long replyToId;
    private Set<Long> readByUsers;
    private Set<Long> deliveredToUsers;
    private LocalDateTime sentAt;
}
