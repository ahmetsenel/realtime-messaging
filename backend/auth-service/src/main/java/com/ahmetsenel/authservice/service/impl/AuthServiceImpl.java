package com.ahmetsenel.authservice.service.impl;

import com.ahmetsenel.authservice.dto.auth.AuthRequest;
import com.ahmetsenel.authservice.dto.auth.LoginResponse;
import com.ahmetsenel.authservice.dto.auth.RegisterResponse;
import com.ahmetsenel.authservice.entity.User;
import com.ahmetsenel.authservice.repository.UserRepository;
import com.ahmetsenel.authservice.security.JwtUtil;
import com.ahmetsenel.authservice.service.AuthService;
import com.ahmetsenel.commonlib.exception.BusinessException;
import com.ahmetsenel.commonlib.exception.MessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public RegisterResponse register(AuthRequest authRequest){
        if (userRepository.existsByUsername(authRequest.getUsername())) {
            throw new BusinessException(MessageType.USERNAME_ALREADY_EXIST);
        }

        User user = User.builder()
                .username(authRequest.getUsername())
                .password(passwordEncoder.encode(authRequest.getPassword()))
                .build();

        User savedUser = userRepository.save(user);

        return new RegisterResponse(savedUser.getUsername());
    }

    public LoginResponse login(AuthRequest authRequest) {
        User user = userRepository.findByUsername(authRequest.getUsername())
                .orElseThrow(() -> new BusinessException(MessageType.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
            throw new BusinessException(MessageType.INVALID_CREDENTIALS);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        return new LoginResponse(token, user.getUsername(), user.getId());
    }
}
