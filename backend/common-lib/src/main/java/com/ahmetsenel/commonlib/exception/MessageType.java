package com.ahmetsenel.commonlib.exception;

import lombok.Getter;

@Getter
public enum MessageType {

    USERNAME_ALREADY_EXIST(1001, 400, "This username is already in use"),
    USER_NOT_FOUND(1002, 404, "User not found"),
    INVALID_CREDENTIALS(1003, 401, "Invalid username or password"),
    TOKEN_GENERATION_FAILED(1004, 500, "A system error occurred while generating the token"),

    GROUP_NOT_FOUND(2001, 404, "Group not found"),
    MESSAGE_NOT_FOUND(2003, 404, "Message not found"),
    INVALID_MESSAGE_TYPE(2004, 400, "Invalid message type"),
    MISSING_RECEIVER_ID(2005, 400, "receiverId is required for DIRECT message"),
    MISSING_GROUP_ID(2006, 400, "groupId is required for GROUP message"),
    USER_NOT_IN_GROUP(2007, 403, "You must be a member of this group to perform actions or send messages"),
    USER_ALREADY_IN_GROUP(2008, 400, "This user is already a member of this group"),

    INTERNAL_SERVER_ERROR(9999, 500, "An unexpected server error occurred");

    private final Integer code;
    private final Integer status;
    private final String message;

    MessageType(Integer code, Integer status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}