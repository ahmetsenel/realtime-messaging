package com.ahmetsenel.chatservice.repository.projection;

public interface UnreadCountProjection {
    Long getSenderId();
    Long getUnreadCount();
}