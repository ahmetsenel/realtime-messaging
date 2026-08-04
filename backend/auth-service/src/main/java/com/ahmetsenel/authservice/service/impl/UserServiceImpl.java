package com.ahmetsenel.authservice.service.impl;

import com.ahmetsenel.authservice.dto.user.UserResponse;
import com.ahmetsenel.authservice.entity.User;
import com.ahmetsenel.authservice.repository.UserRepository;
import com.ahmetsenel.authservice.service.UserService;
import com.ahmetsenel.commonlib.exception.BusinessException;
import com.ahmetsenel.commonlib.exception.MessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public List<UserResponse> searchUsers(String username){

        return userRepository
                .findByUsernameContainingIgnoreCase(username)
                .stream()
                .map(user -> new UserResponse(user.getId(), user.getUsername()))
                .toList();
    }

    public UserResponse getUserById(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MessageType.USER_NOT_FOUND));

        return new UserResponse(user.getId(), user.getUsername());
    }
}