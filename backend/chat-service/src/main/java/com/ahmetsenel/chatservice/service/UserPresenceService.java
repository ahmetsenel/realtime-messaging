package com.ahmetsenel.chatservice.service;

import java.util.List;
import java.util.Map;

public interface UserPresenceService {

    void handleUserOnline(Long userId, String username);

    void handleUserOffline(Long userId, String username);

    boolean isUserOnline(Long userId);

    Map<Long, String> getUserStatuses(Long userId);

    List<String> getOnlinePeers(Long userId);
}
