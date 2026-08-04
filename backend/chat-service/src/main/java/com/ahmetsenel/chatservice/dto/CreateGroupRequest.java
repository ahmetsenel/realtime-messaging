package com.ahmetsenel.chatservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateGroupRequest {
    private String name;
    private List<String> memberIds;
}
