package com.ahmetsenel.chatservice.service.impl;

import com.ahmetsenel.chatservice.dto.CreateGroupRequest;
import com.ahmetsenel.chatservice.dto.GroupMemberResponse;
import com.ahmetsenel.chatservice.dto.GroupSummaryResponse;
import com.ahmetsenel.chatservice.entity.Message;
import com.ahmetsenel.chatservice.entity.ConversationType;
import com.ahmetsenel.chatservice.entity.Group;
import com.ahmetsenel.chatservice.entity.GroupMember;
import com.ahmetsenel.chatservice.mapper.GroupMapper;
import com.ahmetsenel.chatservice.mapper.MessageMapper;
import com.ahmetsenel.chatservice.repository.MessageRepository;
import com.ahmetsenel.chatservice.repository.GroupMemberRepository;
import com.ahmetsenel.chatservice.repository.GroupRepository;
import com.ahmetsenel.chatservice.repository.projection.GroupUnreadCountProjection;
import com.ahmetsenel.chatservice.service.GroupService;
import com.ahmetsenel.chatservice.socket.WebSocketPublisher;
import com.ahmetsenel.commonlib.exception.BusinessException;
import com.ahmetsenel.commonlib.exception.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final MessageRepository messageRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MessageMapper messageMapper;
    private final GroupMapper groupMapper;
    private final WebSocketPublisher webSocketPublisher;

    @Transactional
    public GroupSummaryResponse createGroup(Long creatorId, String creatorUsername, CreateGroupRequest request) {
        Group group = Group.builder()
                .name(request.getName())
                .createdBy(creatorId)
                .build();
        Group savedGroup = groupRepository.save(group);

        GroupMember member = buildGroupMember(savedGroup, creatorId, creatorUsername);
        groupMemberRepository.save(member);

        Message sysMsg = buildSystemMessage(savedGroup, creatorUsername + " created the group");
        messageRepository.save(sysMsg);

        return toSummaryResponse(savedGroup, creatorId);
    }

    @CacheEvict(value = "groupMembership", key = "#groupId + ':' + #userId")
    public GroupMemberResponse addGroupMember(Long groupId, Long userId, String username) {
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId,userId)) {
            throw new BusinessException(MessageType.USER_ALREADY_IN_GROUP);
        }

        Group group = getGroupById(groupId);
        GroupMember member = buildGroupMember(group, userId, username);
        GroupMember savedMember = groupMemberRepository.save(member);

        Message sysMsg = buildSystemMessage(group, username + " was added to the group");
        Message savedMsg = messageRepository.save(sysMsg);

        List<GroupMemberResponse> currentMembers = groupMemberRepository.findByGroupId(group.getId())
                .stream().map(m -> GroupMemberResponse.builder()
                        .userId(m.getUserId()).username(m.getUsername()).joinedAt(m.getJoinedAt()).build())
                .toList();

        webSocketPublisher.publishSystemMessage(messageMapper.toResponse(savedMsg));

        webSocketPublisher.publishGroupAdded(userId,
                Map.of("id", group.getId(),
                        "name", group.getName(),
                        "type", ConversationType.GROUP,
                        "createdBy", group.getCreatedBy(),
                        "members", currentMembers));

        return GroupMemberResponse.builder().userId(savedMember.getUserId()).username(savedMember.getUsername()).joinedAt(savedMember.getJoinedAt()).build();
    }

    @Cacheable(value = "groupMembership", key = "#groupId + ':' + #userId")
    public boolean isMember(Long groupId, Long userId) {
        log.info("Checking membership in the database... Group: {}, User: {}", groupId, userId);
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    public Group getGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(MessageType.GROUP_NOT_FOUND));
    }

    public List<GroupSummaryResponse> getGroupSummariesForUser(Long userId) {

        List<Group> groups = groupRepository.findAllByUserIdWithMembers(userId);

        if (groups.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = groups.stream().map(Group::getId).toList();

        Map<Long, Long> unreadMap = messageRepository.getUnreadCountsForGroups(groupIds, userId)
                .stream()
                .collect(Collectors.toMap(
                        GroupUnreadCountProjection::getGroupId,
                        GroupUnreadCountProjection::getUnreadCount
                ));

        Map<Long, Message> lastMessageMap = messageRepository.findLastMessagesForGroups(groupIds, userId)
                .stream()
                .collect(Collectors.toMap(
                        m -> m.getGroup().getId(),
                        m -> m
                ));

        return groups.stream()
                .map(group -> {
                    long unreadCount = unreadMap.getOrDefault(group.getId(), 0L);
                    Message lastMessage = lastMessageMap.get(group.getId());

                    return groupMapper.toGroupSummaryResponse(group, group.getMembers(), unreadCount, lastMessage);
                })
                .toList();
    }

    private GroupMember buildGroupMember(Group group, Long userId, String username) {
        return GroupMember.builder()
                .group(group)
                .userId(userId)
                .username(username)
                .build();
    }

    private Message buildSystemMessage(Group group, String content) {
        return Message.builder()
                .type(ConversationType.SYSTEM)
                .group(group)
                .senderId(0L)
                .senderUsername("Sistem")
                .content(content)
                .build();
    }

    private GroupSummaryResponse toSummaryResponse(Group group, Long currentUserId) {

        List<GroupMember> members = groupMemberRepository.findByGroupId(group.getId());
        long unread = messageRepository.countUnreadGroupMessages(group.getId(), currentUserId);

        List<Message> lastMessages = messageRepository.findGroupMessagesForUser(group.getId(), currentUserId, PageRequest.of(0, 1));
        Message lastMessage = lastMessages.isEmpty() ? null : lastMessages.get(0);

        return groupMapper.toGroupSummaryResponse(group, members, unread, lastMessage);
    }
}