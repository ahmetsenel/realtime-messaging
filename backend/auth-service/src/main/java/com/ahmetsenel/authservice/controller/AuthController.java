package com.ahmetsenel.authservice.controller;

import com.ahmetsenel.authservice.dto.auth.AuthRequest;
import com.ahmetsenel.authservice.dto.auth.LoginResponse;
import com.ahmetsenel.authservice.dto.auth.RegisterResponse;
import com.ahmetsenel.authservice.service.AuthService;
import com.ahmetsenel.commonlib.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@RequestBody AuthRequest authRequest) {
        return ApiResponse.ok(authService.register(authRequest));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody AuthRequest authRequest) {
        return ApiResponse.ok(authService.login(authRequest));
    }
}
