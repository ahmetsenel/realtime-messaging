package com.ahmetsenel.chatservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor != null &&
                StompCommand.CONNECT.equals(accessor.getCommand())) {

            try {
                List<String> authHeaders = accessor.getNativeHeader("Authorization");

                if (authHeaders == null || authHeaders.isEmpty()) {
                    log.warn("WebSocket Auth Failed: Missing Authorization header");
                    throw new IllegalArgumentException("Missing Authorization header");
                }

                String token = authHeaders.get(0).replace("Bearer ", "");

                if (!jwtUtil.validateToken(token)) {
                    log.warn("WebSocket Auth Failed: Invalid or expired JWT token");
                    throw new IllegalArgumentException("Invalid or expired JWT token");
                }

                Long userId = jwtUtil.extractUserId(token);
                String username = jwtUtil.extractUsername(token);

                accessor.setUser(
                        new UsernamePasswordAuthenticationToken(
                                new UserPrincipal(userId, username),
                                null,
                                Collections.emptyList()
                        )
                );

                log.info("WEBSOCKET AUTH OK - User: {}", username);

            } catch (Exception e) {
                log.error("WebSocket Auth Failed: Error extracting token claims", e);
                throw new IllegalArgumentException("Unauthorized WebSocket access");
            }
        }
        return message;
    }

}
