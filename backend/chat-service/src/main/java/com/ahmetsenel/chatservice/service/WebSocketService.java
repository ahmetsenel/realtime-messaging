package com.ahmetsenel.chatservice.service;

import com.ahmetsenel.chatservice.dto.*;

public interface WebSocketService {

    void processSendMessage(SendMessageRequest request, Long userId, String username);

    void processReadReceipt(ReadReceiptRequest request, Long userId);

    void processTypingSignal(TypingRequest request, Long userId, String username);

    void processDeliveryReceipt(ReadReceiptRequest request, Long userId);

    void processDeleteMessage(DeleteMessageRequest request, Long userId);
}
