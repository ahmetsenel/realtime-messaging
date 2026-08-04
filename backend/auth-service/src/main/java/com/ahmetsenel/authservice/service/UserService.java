package com.ahmetsenel.authservice.service;

import com.ahmetsenel.authservice.dto.user.UserResponse;
import java.util.List;

public interface UserService {

    List<UserResponse> searchUsers(String username);

    UserResponse getUserById(Long id);
}
