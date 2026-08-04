package com.ahmetsenel.chatservice.mapper;

import com.ahmetsenel.chatservice.dto.GroupMemberResponse;
import com.ahmetsenel.chatservice.dto.GroupSummaryResponse;
import com.ahmetsenel.chatservice.entity.Group;
import com.ahmetsenel.chatservice.entity.GroupMember;
import com.ahmetsenel.chatservice.entity.Message;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class GroupMapper {

    public GroupSummaryResponse toGroupSummaryResponse(Group group,
                                                       List<GroupMember> members,
                                                       long unreadCount,
                                                       Message lastMessage) {

        List<GroupMemberResponse> memberResponses = members.stream()
                .map(this::toGroupMemberResponse)
                .toList();

        String displayMessage = lastMessage != null ? lastMessage.getContent() : "Grup oluşturuldu";
        String displaySender = (lastMessage != null && lastMessage.getSenderId() != 0) ? lastMessage.getSenderUsername() : null;
        LocalDateTime displayTime = lastMessage != null ? lastMessage.getSentAt() : group.getCreatedAt();

        return GroupSummaryResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .members(memberResponses)
                .unreadCount(unreadCount)
                .lastMessage(displayMessage)
                .lastSender(displaySender)
                .lastMessageTime(displayTime)
                .build();
    }

    public GroupMemberResponse toGroupMemberResponse(GroupMember member) {
        return GroupMemberResponse.builder()
                .userId(member.getUserId())
                .username(member.getUsername())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
