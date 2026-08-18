package com.ahmetsenel.chatservice.service.impl;

import com.ahmetsenel.chatservice.entity.UserStatus;
import com.ahmetsenel.chatservice.repository.MessageRepository;
import com.ahmetsenel.chatservice.repository.UserStatusRepository;
import com.ahmetsenel.chatservice.service.UserPresenceService;
import com.ahmetsenel.chatservice.socket.WebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPresenceServiceImpl implements UserPresenceService {

    private final MessageRepository messageRepository;
    private final UserStatusRepository userStatusRepository;
    private final WebSocketPublisher webSocketPublisher;
    private final StringRedisTemplate redisTemplate;

    private static final String ONLINE_USERS_KEY = "users:online";

    public void handleUserOnline(Long userId, String username) {
        log.info("User online: {}", username);

        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, String.valueOf(userId));

        webSocketPublisher.publishUserOnline(userId);
    }

    public void handleUserOffline(Long userId, String username) {
        log.info("User offline: {}", username);

        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, String.valueOf(userId));

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
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, String.valueOf(userId)));
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

        List<String> onlinePeers = new ArrayList<>();

        for (Long peerId : peerIds) {
            if (isUserOnline(peerId)) {
                onlinePeers.add(String.valueOf(peerId));
            }
        }
        return onlinePeers;
    }

    private Set<Long> getDirectMessagingPeerIds(Long userId) {
        Set<Long> peerIds = new HashSet<>();

        messageRepository.findLatestDirectMessagesForUser(userId).forEach(m -> {
            peerIds.add(m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId());
        });

        return peerIds;
    }

}
