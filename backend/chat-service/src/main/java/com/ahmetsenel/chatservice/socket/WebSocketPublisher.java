package com.ahmetsenel.chatservice.socket;

import com.ahmetsenel.chatservice.dto.MessageResponse;
import com.ahmetsenel.chatservice.entity.ConversationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishMessage(MessageResponse message, Long senderId) {
        if (message.getType() == ConversationType.GROUP) {
            publishGroupMessage(message);
        }
        else
            publishDirectMessage(senderId, message.getReceiverId(), message);
    }

    private void publishDirectMessage(Long senderId, Long receiverId, MessageResponse message) {
        sendToUser(receiverId, "/queue/messages", message);
        sendToUser(senderId, "/queue/messages", message);
    }

    private void publishGroupMessage(MessageResponse message) {
        sendToGroup(message.getGroupId(), "", message);
    }

    public void publishSystemMessage(MessageResponse message) {
        if (message.getGroupId() != null) {
            sendToGroup(message.getGroupId(), "", message);
        } else if (message.getReceiverId() != null) {
            sendToUser(message.getReceiverId(), "/queue/messages", message);
        }
    }

    public void publishDirectRead(Long senderId, Long readerId) {
        sendToUser(senderId, "/queue/read",
                Map.of(
                        "type", ConversationType.DIRECT,
                        "readerId", readerId
                )
        );
    }

    public void publishGroupRead(Long groupId, Long readerId) {
        sendToGroup(groupId, "/read",
                Map.of(
                        "type", ConversationType.GROUP,
                        "groupId", groupId,
                        "readerId", readerId
                )
        );
    }

    public void publishDirectDelivered(Long senderId, Long delivererId) {
        sendToUser(senderId, "/queue/deliver",
                Map.of(
                        "type", ConversationType.DIRECT,
                        "delivererId", delivererId
                )
        );
    }

    public void publishGroupDelivered(Long groupId, Long delivererId) {
        sendToGroup(groupId, "/deliver",
                Map.of(
                        "type", ConversationType.GROUP,
                        "groupId", groupId,
                        "delivererId", delivererId
                )
        );
    }

    public void publishDirectTyping(Long receiverId, Long typerId, String typerName) {
        sendToUser(receiverId, "/queue/typing",
                Map.of(
                        "type", ConversationType.DIRECT,
                        "typerId", typerId,
                        "typerName", typerName
                )
        );
    }

    public void publishGroupTyping(Long groupId, Long typerId, String typerName) {
        sendToGroup(groupId, "/typing",
                Map.of(
                        "type", ConversationType.GROUP,
                        "groupId", groupId,
                        "typerId", typerId,
                        "typerName", typerName
                )
        );
    }

    public void publishDirectDeleteToPeer(Long peerUserId, Long messageId, Long deletedBy) {
        sendToUser(peerUserId, "/queue/delete",
                Map.of(
                        "type", ConversationType.DIRECT,
                        "messageId", messageId,
                        "peerId", deletedBy
                )
        );
    }

    public void publishDirectDeleteToOwner(Long userId, Long messageId, Long peerUserId) {
        sendToUser(userId, "/queue/delete",
                Map.of(
                        "type", ConversationType.DIRECT,
                        "messageId", messageId,
                        "peerId", peerUserId
                )
        );
    }

    public void publishGroupDelete(Long groupId, Long messageId) {
        sendToGroup(groupId, "/delete",
                Map.of(
                        "type", ConversationType.GROUP,
                        "groupId", groupId,
                        "messageId", messageId
                )
        );
    }

    public void publishGroupAdded(Long userId, Map<String, Object> payload) {
        sendToUser(userId, "/queue/group.added", payload);
    }

    public void publishUserOnline(Long userId) {
        messagingTemplate.convertAndSend("/topic/user.status", Map.of(
                "userId", userId,
                "status", "ONLINE"
        ));
    }

    public void publishUserOffline(Long userId, java.time.Instant lastSeen) {
        messagingTemplate.convertAndSend("/topic/user.status", Map.of(
                "userId", userId,
                "status", "OFFLINE",
                "lastSeen", lastSeen.toString()
        ));
    }

    private void sendToUser(Long userId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                destination,
                payload
        );
    }


    private void sendToGroup(Long groupId, String destination, Object payload) {
        messagingTemplate.convertAndSend(
                "/topic/group/" + groupId + destination,
                payload);
    }
}
