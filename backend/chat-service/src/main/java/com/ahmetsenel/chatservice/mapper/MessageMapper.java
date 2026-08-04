package com.ahmetsenel.chatservice.mapper;

import com.ahmetsenel.chatservice.dto.DirectSummaryResponse;
import com.ahmetsenel.chatservice.dto.MessageResponse;
import com.ahmetsenel.chatservice.entity.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .type(message.getType())
                .senderId(message.getSenderId())
                .senderUsername(message.getSenderUsername())
                .receiverId(message.getReceiverId())
                .receiverUsername(message.getReceiverUsername())
                .groupId(message.getGroup() != null ? message.getGroup().getId() : null)
                .content(message.getContent())
                .read(message.isRead())
                .delivered(message.isDelivered())
                .deliveredToUsers(message.getDeliveredToUsers())
                .readByUsers(message.getReadByUsers())
                .deleted(message.isDeleted())
                .replyToId(message.getReplyToId())
                .sentAt(message.getSentAt())
                .build();
    }

    public List<MessageResponse> toResponseList(List<Message> messages) {
        return messages.stream()
                .map(this::toResponse)
                .toList();
    }

    public DirectSummaryResponse toDirectSummaryResponse(Message message, Long userId, Map<Long, Long> unreadMap) {
        boolean sentByMe = message.getSenderId().equals(userId);
        Long peerId = sentByMe ? message.getReceiverId() : message.getSenderId();
        String peerUsername = sentByMe ? message.getReceiverUsername() : message.getSenderUsername();

        return DirectSummaryResponse.builder()
                .key("DIRECT_" + peerId)
                .type("DIRECT")
                .id(peerId)
                .name(peerUsername)
                .lastMessage(message.getContent())
                .lastTime(message.getSentAt())
                .lastSender(message.getSenderUsername())
                .unread(unreadMap.getOrDefault(peerId, 0L))
                .build();
    }
}
