package com.ahmetsenel.chatservice.socket;

import com.ahmetsenel.chatservice.security.UserPrincipal;
import com.ahmetsenel.chatservice.service.impl.UserPresenceServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UserPresenceServiceImpl userPresenceService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        UserPrincipal user = getUserFromEvent(event.getMessage());
        if (user != null) {
            userPresenceService.handleUserOnline(user.userId(), user.username());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        UserPrincipal user = getUserFromEvent(event.getMessage());
        if (user != null) {
            userPresenceService.handleUserOffline(user.userId(), user.username());
        }
    }

    private UserPrincipal getUserFromEvent(Message<?> message) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
        Principal principal = headerAccessor.getUser();

        if (principal instanceof Authentication authentication) {
            return (UserPrincipal) authentication.getPrincipal();
        }
        return null;
    }
}