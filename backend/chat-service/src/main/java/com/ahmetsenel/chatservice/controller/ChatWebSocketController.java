package com.ahmetsenel.chatservice.controller;

import com.ahmetsenel.chatservice.dto.*;
import com.ahmetsenel.chatservice.security.UserPrincipal;
import com.ahmetsenel.chatservice.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final WebSocketService webSocketService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        UserPrincipal user = getCurrentUser(principal);

        webSocketService.processSendMessage(request, user.userId(), user.username());
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload ReadReceiptRequest request, Principal principal) {
        UserPrincipal user = getCurrentUser(principal);

        webSocketService.processReadReceipt(request, user.userId());
    }

    @MessageMapping("/chat.typing")
    public void typingSignal(@Payload TypingRequest request, Principal principal) {
        UserPrincipal user = getCurrentUser(principal);

        webSocketService.processTypingSignal(request, user.userId(), user.username());
    }

    @MessageMapping("/chat.deliver")
    public void markAsDelivered(@Payload ReadReceiptRequest request, Principal principal) {
        UserPrincipal user = getCurrentUser(principal);

        webSocketService.processDeliveryReceipt(request, user.userId());
    }

    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload DeleteMessageRequest request, Principal principal) {
        UserPrincipal user = getCurrentUser(principal);

        webSocketService.processDeleteMessage(request, user.userId());
    }

    private UserPrincipal getCurrentUser(Principal principal) {
        Authentication authentication = (Authentication) principal;
        return (UserPrincipal) authentication.getPrincipal();
    }
}