package com.ahmetsenel.chatservice.service.impl;

import com.ahmetsenel.chatservice.dto.DirectSummaryResponse;
import com.ahmetsenel.chatservice.dto.MessageResponse;
import com.ahmetsenel.chatservice.dto.SendMessageRequest;
import com.ahmetsenel.chatservice.entity.GroupMember;
import com.ahmetsenel.chatservice.entity.Message;
import com.ahmetsenel.chatservice.entity.Group;
import com.ahmetsenel.chatservice.mapper.MessageMapper;
import com.ahmetsenel.chatservice.repository.MessageRepository;
import com.ahmetsenel.chatservice.repository.projection.UnreadCountProjection;
import com.ahmetsenel.chatservice.service.ChatService;
import com.ahmetsenel.chatservice.service.GroupService;
import com.ahmetsenel.chatservice.service.UserPresenceService;
import com.ahmetsenel.commonlib.exception.BusinessException;
import com.ahmetsenel.commonlib.exception.MessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final GroupService groupService;
    private final MessageMapper messageMapper;
    private final UserPresenceService userPresenceService;


    @Transactional
    public MessageResponse sendMessage(Long senderId, String senderUsername, SendMessageRequest request) {

        Message.MessageBuilder builder = buildBaseMessage(senderId, senderUsername, request);

        switch (request.getType()) {
            case DIRECT -> buildDirectMessage(builder, senderId, request);
            case GROUP -> buildGroupMessage(builder, senderId, request);
            default -> throw new BusinessException(MessageType.INVALID_MESSAGE_TYPE);
        }

        Message saved = messageRepository.save(builder.build());
        return messageMapper.toResponse(saved);
    }

    public List<MessageResponse> getDirectMessages(Long userId, Long withUserId, Pageable pageable) {
        List<Message> messages = messageRepository.findDirectMessagesPaged(userId, withUserId, pageable);
        return messageMapper.toResponseList(messages);
    }

    public List<MessageResponse> getGroupMessages(Long groupId, Long userId, Pageable pageable) {
        List<Message> messages = messageRepository.findGroupMessagesForUser(groupId, userId, pageable);
        return messageMapper.toResponseList(messages);
    }

    public List<DirectSummaryResponse> getDirectSummaries(Long userId) {

        Map<Long, Long> unreadMap = messageRepository
                .getUnreadCountsGroupedBySender(userId)
                .stream()
                .collect(Collectors.toMap(
                        UnreadCountProjection::getSenderId,
                        UnreadCountProjection::getUnreadCount
                ));

        return getLatestDirectMessages(userId)
                .stream()
                .map(m -> messageMapper.toDirectSummaryResponse(m, userId, unreadMap))
                .toList();
    }

    @Transactional
    public void markDirectMessageAsRead(Long senderId, Long receiverId) {
        messageRepository.markDirectMessagesAsRead(senderId, receiverId);
    }

    @Transactional
    public void markGroupMessageAsRead(Long groupId, Long userId) {
        List<Message> unreadMessages = messageRepository.findUnreadGroupMessagesForUser(groupId, userId);
        updateGroupMessageStatus(unreadMessages, groupId, userId, Message::getReadByUsers,
                m -> m.setRead(true));
    }

    @Transactional
    public void markDirectMessageAsDelivered(Long senderId, Long receiverId) {
        messageRepository.markDirectMessagesAsDelivered(senderId, receiverId);
    }

    @Transactional
    public void markGroupMessageAsDelivered(Long groupId, Long userId) {
        List<Message> undeliveredMessages = messageRepository.findUndeliveredGroupMessagesForUser(groupId, userId);
        updateGroupMessageStatus(undeliveredMessages, groupId, userId, Message::getDeliveredToUsers,
                m -> m.setDelivered(true));
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(MessageType.MESSAGE_NOT_FOUND));

        if (msg.getSenderId().equals(userId)) {
            msg.setDeleted(true);
            messageRepository.save(msg);
        }
    }

    private List<Message> getLatestDirectMessages(Long userId) {
        return messageRepository.findLatestDirectMessagesForUser(userId);
    }

    private void updateGroupMessageStatus(List<Message> messages, Long groupId, Long userId,
                                          Function<Message, Set<Long>> statusSetGetter,
                                          Consumer<Message> statusUpdater) {
        if (messages.isEmpty()) return;

        Group group = groupService.getGroupById(groupId);
        int requiredReads = group.getMembers().size() - 1;

        for (Message m : messages) {
            statusSetGetter.apply(m).add(userId);
            if (statusSetGetter.apply(m).size() >= requiredReads) {
                statusUpdater.accept(m);
            }
        }
        messageRepository.saveAll(messages);
    }

    private Message.MessageBuilder buildBaseMessage(Long senderId,
                                                    String senderUsername,
                                                    SendMessageRequest request) {

        return Message.builder()
                .type(request.getType())
                .senderId(senderId)
                .senderUsername(senderUsername)
                .content(request.getContent())
                .replyToId(request.getReplyToId());
    }

    private void buildDirectMessage(Message.MessageBuilder builder, Long senderId, SendMessageRequest request) {

        if (request.getReceiverId() == null) {
            throw new BusinessException(MessageType.MISSING_RECEIVER_ID);
        }
        builder.receiverId(request.getReceiverId())
                .receiverUsername(request.getReceiverUsername());

        if (senderId.equals(request.getReceiverId())) {
            builder.delivered(true).read(true);
        } else {
            boolean isOnline = userPresenceService.isUserOnline(request.getReceiverId());
            builder.delivered(isOnline);
        }
    }

    private void buildGroupMessage(Message.MessageBuilder builder, Long senderId, SendMessageRequest request) {
        if (request.getGroupId() == null) {
            throw new BusinessException(MessageType.MISSING_GROUP_ID);
        }

        if (!groupService.isMember(request.getGroupId(), senderId)) {
            throw new BusinessException(MessageType.USER_NOT_IN_GROUP);
        }

        Group group = groupService.getGroupById(request.getGroupId());
        builder.group(group);

        int required = group.getMembers().size() - 1;

        if (required <= 0) {
            builder.delivered(true).read(true);
        } else {
            Set<Long> deliveredUsers = new HashSet<>();
            for (GroupMember member : group.getMembers()) {
                if (!member.getUserId().equals(senderId)) {
                    if (userPresenceService.isUserOnline(member.getUserId())) {
                        deliveredUsers.add(member.getUserId());
                    }
                }
            }
            builder.deliveredToUsers(deliveredUsers);
            if (deliveredUsers.size() >= required) {
                builder.delivered(true);
            }
        }
    }
}
