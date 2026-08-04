package com.ahmetsenel.chatservice.service;

import com.ahmetsenel.chatservice.dto.DirectSummaryResponse;
import com.ahmetsenel.chatservice.dto.MessageResponse;
import com.ahmetsenel.chatservice.dto.SendMessageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ChatService {

    MessageResponse sendMessage(Long senderId, String senderUsername, SendMessageRequest request);

    List<MessageResponse> getDirectMessages(Long userId, Long withUserId, Pageable pageable
    );

    List<MessageResponse> getGroupMessages(Long groupId, Long userId, Pageable pageable
    );

    List<DirectSummaryResponse> getDirectSummaries(Long userId);

    void markDirectMessageAsRead(Long senderId, Long receiverId);

    void markGroupMessageAsRead(Long groupId, Long userId);

    void markDirectMessageAsDelivered(Long senderId, Long receiverId);

    void markGroupMessageAsDelivered(Long groupId, Long userId);

    void deleteMessage(Long messageId, Long userId);
}
