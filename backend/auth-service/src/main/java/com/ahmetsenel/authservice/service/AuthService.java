package com.ahmetsenel.authservice.service;

import com.ahmetsenel.authservice.dto.auth.AuthRequest;
import com.ahmetsenel.authservice.dto.auth.LoginResponse;
import com.ahmetsenel.authservice.dto.auth.RegisterResponse;

public interface AuthService {

    RegisterResponse register(AuthRequest authRequest);

    LoginResponse login(AuthRequest authRequest);
}
