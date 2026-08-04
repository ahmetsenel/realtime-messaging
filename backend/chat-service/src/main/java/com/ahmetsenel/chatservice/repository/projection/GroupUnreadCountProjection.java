package com.ahmetsenel.chatservice.repository.projection;

public interface GroupUnreadCountProjection {
    Long getGroupId();
    Long getUnreadCount();
}