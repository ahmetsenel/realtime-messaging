package com.ahmetsenel.chatservice.service;

import com.ahmetsenel.chatservice.dto.CreateGroupRequest;
import com.ahmetsenel.chatservice.dto.GroupMemberResponse;
import com.ahmetsenel.chatservice.dto.GroupSummaryResponse;
import com.ahmetsenel.chatservice.entity.Group;
import java.util.List;

public interface GroupService {

    GroupSummaryResponse createGroup(Long creatorId, String creatorUsername, CreateGroupRequest request);

    GroupMemberResponse addGroupMember(Long groupId, Long userId, String username);

    boolean isMember(Long groupId, Long userId);

    Group getGroupById(Long groupId);

    List<GroupSummaryResponse> getGroupSummariesForUser(Long userId);
}
