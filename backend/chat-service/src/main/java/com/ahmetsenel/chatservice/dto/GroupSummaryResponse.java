package com.ahmetsenel.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSummaryResponse {
    private Long id;
    private String name;
    private Long createdBy;
    private LocalDateTime createdAt;
    private List<GroupMemberResponse> members;

    private long unreadCount;

    private String lastMessage;
    private String lastSender;
    private LocalDateTime lastMessageTime;
}
