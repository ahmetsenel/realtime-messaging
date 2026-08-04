package com.ahmetsenel.chatservice.service.impl;

import com.ahmetsenel.chatservice.entity.UserStatus;
import com.ahmetsenel.chatservice.repository.MessageRepository;
import com.ahmetsenel.chatservice.repository.UserStatusRepository;
import com.ahmetsenel.chatservice.service.UserPresenceService;
import com.ahmetsenel.chatservice.socket.WebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPresenceServiceImpl implements UserPresenceService {

    private final SimpUserRegistry simpUserRegistry;
    private final MessageRepository messageRepository;
    private final UserStatusRepository userStatusRepository;
    private final WebSocketPublisher webSocketPublisher;

    public void handleUserOnline(Long userId, String username) {
        log.info("User online: {}", username);
        webSocketPublisher.publishUserOnline(userId);
    }

    public void handleUserOffline(Long userId, String username) {
        log.info("User offline: {}", username);
        Instant now = Instant.now();

        UserStatus userStatus = userStatusRepository.findById(userId)
                .orElse(UserStatus.builder()
                                .userId(userId)
                                .lastSeen(now)
                                .build()
                );

        userStatusRepository.save(userStatus);

        webSocketPublisher.publishUserOffline(userId, now);
    }

    public boolean isUserOnline(Long userId) {
        return simpUserRegistry.getUser(String.valueOf(userId)) != null;
    }

    public Map<Long, String> getUserStatuses(Long userId) {
        Set<Long> peerIds = getDirectMessagingPeerIds(userId);

        if(peerIds.isEmpty()) return Map.of();

        return userStatusRepository.findAllById(peerIds).stream()
                .collect(Collectors.toMap(
                        com.ahmetsenel.chatservice.entity.UserStatus::getUserId,
                        s -> s.getLastSeen().toString()
                ));
    }

    public List<String> getOnlinePeers(Long userId) {
        Set<Long> peerIds = getDirectMessagingPeerIds(userId);

        if (peerIds.isEmpty()) {
            return List.of();
        }

        return simpUserRegistry.getUsers().stream()
                .map(SimpUser::getName)
                .filter(name -> {
                    try {
                        return peerIds.contains(Long.parseLong(name));
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .toList();
    }

    private Set<Long> getDirectMessagingPeerIds(Long userId) {
        Set<Long> peerIds = new HashSet<>();

        messageRepository.findLatestDirectMessagesForUser(userId).forEach(m -> {
            peerIds.add(m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId());
        });

        return peerIds;
    }

}
