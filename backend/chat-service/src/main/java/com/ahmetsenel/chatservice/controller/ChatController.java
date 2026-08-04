package com.ahmetsenel.chatservice.controller;

import com.ahmetsenel.chatservice.dto.*;
import com.ahmetsenel.chatservice.security.UserPrincipal;
import com.ahmetsenel.chatservice.service.ChatService;
import com.ahmetsenel.chatservice.service.GroupService;
import com.ahmetsenel.chatservice.service.UserPresenceService;
import com.ahmetsenel.commonlib.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final GroupService groupService;
    private final UserPresenceService userPresenceService;

    @GetMapping("/direct")
    public ApiResponse<List<DirectSummaryResponse>> getSummaryDirectMessages(
            @AuthenticationPrincipal UserPrincipal user) {

        return ApiResponse.ok(chatService.getDirectSummaries(user.userId()));
    }

    @GetMapping("/direct/{withUserId}/messages")
    public ApiResponse<List<MessageResponse>> getDirectMessages(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long withUserId,
            @PageableDefault(size = 50) Pageable pageable) {

        return ApiResponse.ok(chatService.getDirectMessages(user.userId(), withUserId, pageable));
    }

    @PostMapping("/groups")
    public ApiResponse<GroupSummaryResponse> createGroup(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody CreateGroupRequest request) {

        return ApiResponse.ok(groupService.createGroup(user.userId(), user.username(), request));
    }

    @GetMapping("/groups")
    public ApiResponse<List<GroupSummaryResponse>> getSummaryGroups(
            @AuthenticationPrincipal UserPrincipal user) {

        return ApiResponse.ok(groupService.getGroupSummariesForUser(user.userId()));
    }

    @GetMapping("/groups/{groupId}/messages")
    public ApiResponse<List<MessageResponse>> getGroupMessages(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long groupId,
            @PageableDefault(size = 50) Pageable pageable) {

        return ApiResponse.ok(chatService.getGroupMessages(groupId, user.userId(), pageable));
    }

    @PostMapping("/groups/{groupId}/members")
    public ApiResponse<GroupMemberResponse> addMember(
            @PathVariable Long groupId,
            @RequestBody AddMemberRequest request) {

        return ApiResponse.ok(groupService.addGroupMember(groupId, request.getUserId(), request.getUsername()));
    }

    @GetMapping("/users/statuses")
    public ApiResponse<Map<Long, String>> getUserStatuses(
            @AuthenticationPrincipal UserPrincipal user) {

        return ApiResponse.ok(userPresenceService.getUserStatuses(user.userId()));
    }

    @GetMapping("/users/online")
    public ApiResponse<List<String>> activeUsers(
            @AuthenticationPrincipal UserPrincipal user) {

        return ApiResponse.ok(userPresenceService.getOnlinePeers(user.userId()));
    }
}