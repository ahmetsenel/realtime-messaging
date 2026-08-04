package com.ahmetsenel.authservice.controller;

import com.ahmetsenel.authservice.dto.user.UserResponse;
import com.ahmetsenel.authservice.service.UserService;
import com.ahmetsenel.commonlib.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    public ApiResponse<List<UserResponse>> search(@RequestParam String username){
        return ApiResponse.ok(userService.searchUsers(username));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id){
        return ApiResponse.ok(userService.getUserById(id));
    }
}
