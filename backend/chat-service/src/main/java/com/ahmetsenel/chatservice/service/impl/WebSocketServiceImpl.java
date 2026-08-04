package com.ahmetsenel.chatservice.service.impl;

import com.ahmetsenel.chatservice.service.ChatService;
import com.ahmetsenel.chatservice.service.WebSocketService;
import com.ahmetsenel.chatservice.socket.WebSocketPublisher;
import lombok.RequiredArgsConstructor;
import com.ahmetsenel.chatservice.dto.DeleteMessageRequest;
import com.ahmetsenel.chatservice.dto.ReadReceiptRequest;
import com.ahmetsenel.chatservice.dto.SendMessageRequest;
import com.ahmetsenel.chatservice.dto.TypingRequest;
import com.ahmetsenel.chatservice.dto.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final ChatService chatService;
    private final WebSocketPublisher webSocketPublisher;

    public void processSendMessage(SendMessageRequest request, Long userId, String username) {
        MessageResponse saved = chatService.sendMessage(userId, username, request);
        webSocketPublisher.publishMessage(saved, userId);
        log.info("Message sent by {} -> type={}, group={}, receiver={}",
                username, saved.getType(), saved.getGroupId(), saved.getReceiverId());
    }

    public void processReadReceipt(ReadReceiptRequest request, Long userId) {
        if ("DIRECT".equals(request.getType())) {
            chatService.markDirectMessageAsRead(request.getId(), userId);
            webSocketPublisher.publishDirectRead(request.getId(), userId);
        } else if ("GROUP".equals(request.getType())) {
            chatService.markGroupMessageAsRead(request.getId(), userId);
            webSocketPublisher.publishGroupRead(request.getId(), userId);
        }
    }

    public void processTypingSignal(TypingRequest request, Long userId, String username) {
        if ("DIRECT".equals(request.getType())) {
            webSocketPublisher.publishDirectTyping(request.getId(), userId, username);
        } else if ("GROUP".equals(request.getType())) {
            webSocketPublisher.publishGroupTyping(request.getId(), userId, username);
        }
    }

    public void processDeliveryReceipt(ReadReceiptRequest request, Long userId) {
        if ("DIRECT".equals(request.getType())) {
            chatService.markDirectMessageAsDelivered(request.getId(), userId);
            webSocketPublisher.publishDirectDelivered(request.getId(), userId);
        } else if ("GROUP".equals(request.getType())) {
            chatService.markGroupMessageAsDelivered(request.getId(), userId);
            webSocketPublisher.publishGroupDelivered(request.getId(), userId);
        }
    }

    public void processDeleteMessage(DeleteMessageRequest request, Long userId) {
        chatService.deleteMessage(request.getMessageId(), userId);

        if ("DIRECT".equals(request.getType())) {
            webSocketPublisher.publishDirectDeleteToPeer(request.getId(), request.getMessageId(), userId);
            webSocketPublisher.publishDirectDeleteToOwner(userId, request.getMessageId(), request.getId());
        } else if ("GROUP".equals(request.getType())) {
            webSocketPublisher.publishGroupDelete(request.getId(), request.getMessageId());
        }
    }
}
