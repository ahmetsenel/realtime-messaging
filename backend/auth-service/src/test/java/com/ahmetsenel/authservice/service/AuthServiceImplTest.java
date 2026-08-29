package com.ahmetsenel.authservice.service;

import com.ahmetsenel.authservice.dto.auth.AuthRequest;
import com.ahmetsenel.authservice.dto.auth.LoginResponse;
import com.ahmetsenel.authservice.dto.auth.RegisterResponse;
import com.ahmetsenel.authservice.entity.User;
import com.ahmetsenel.authservice.repository.UserRepository;
import com.ahmetsenel.authservice.security.JwtUtil;
import com.ahmetsenel.authservice.service.impl.AuthServiceImpl;
import com.ahmetsenel.commonlib.exception.BusinessException;
import com.ahmetsenel.commonlib.exception.MessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthServiceImpl authService;

    @Captor ArgumentCaptor<User> userCaptor;

    // ─── Test fixtures ────────────────────────────────────────────────────────

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "ahmet";
    private static final String RAW_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword123";
    private static final String TOKEN = "jwt.token.123";

    private User buildUser() {
        return User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .password(ENCODED_PASSWORD)
                .build();
    }

    private AuthRequest authRequest() {
        AuthRequest req = new AuthRequest();
        req.setUsername(USERNAME);
        req.setPassword(RAW_PASSWORD);
        return req;
    }

    // ─── Register Tests ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("successfully registers a new user and returns username")
        void register_happyPath_returnsRegisterResponse() {
            // given
            AuthRequest request = authRequest();
            User savedUser = buildUser();

            given(userRepository.existsByUsername(USERNAME)).willReturn(false);
            given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            RegisterResponse result = authService.register(request);

            // then
            assertThat(result.getUsername()).isEqualTo(USERNAME);

            then(userRepository).should().save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();

            assertThat(capturedUser.getUsername()).isEqualTo(USERNAME);
            assertThat(capturedUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("throws BusinessException when username already exists")
        void register_usernameTaken_throwsException() {
            // given
            AuthRequest request = authRequest();
            given(userRepository.existsByUsername(USERNAME)).willReturn(true);

            // when / then
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(MessageType.USERNAME_ALREADY_EXIST.getMessage());

            then(passwordEncoder).shouldHaveNoInteractions();
            then(userRepository).should(never()).save(any());
        }
    }

    // ─── Login Tests ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("successfully logs in and returns token with user details")
        void login_validCredentials_returnsLoginResponse() {
            // given
            AuthRequest request = authRequest();
            User user = buildUser();

            given(userRepository.findByUsername(USERNAME)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(jwtUtil.generateToken(USER_ID, USERNAME)).willReturn(TOKEN);

            // when
            LoginResponse result = authService.login(request);

            // then
            assertThat(result.getToken()).isEqualTo(TOKEN);
            assertThat(result.getUsername()).isEqualTo(USERNAME);
            assertThat(result.getUserId()).isEqualTo(USER_ID);

            then(jwtUtil).should().generateToken(USER_ID, USERNAME);
        }

        @Test
        @DisplayName("throws BusinessException when user is not found")
        void login_userNotFound_throwsException() {
            // given
            AuthRequest request = authRequest();
            given(userRepository.findByUsername(USERNAME)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(MessageType.INVALID_CREDENTIALS.getMessage());

            then(passwordEncoder).shouldHaveNoInteractions();
            then(jwtUtil).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("throws BusinessException when password does not match")
        void login_invalidPassword_throwsException() {
            // given
            AuthRequest request = authRequest();
            User user = buildUser();

            given(userRepository.findByUsername(USERNAME)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(false);

            // when / then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(MessageType.INVALID_CREDENTIALS.getMessage());

            then(jwtUtil).shouldHaveNoInteractions();
        }
    }
}