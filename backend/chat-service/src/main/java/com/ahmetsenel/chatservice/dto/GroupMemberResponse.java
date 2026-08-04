package com.ahmetsenel.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberResponse {
    private Long userId;
    private String username;
    private LocalDateTime joinedAt;
}
