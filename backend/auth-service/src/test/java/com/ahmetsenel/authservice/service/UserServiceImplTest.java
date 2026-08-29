package com.ahmetsenel.authservice.service;

import com.ahmetsenel.authservice.dto.user.UserResponse;
import com.ahmetsenel.authservice.entity.User;
import com.ahmetsenel.authservice.repository.UserRepository;
import com.ahmetsenel.authservice.service.impl.UserServiceImpl;
import com.ahmetsenel.commonlib.exception.BusinessException;
import com.ahmetsenel.commonlib.exception.MessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Tests")
class UserServiceImplTest {

    @Mock UserRepository userRepository;

    @InjectMocks UserServiceImpl userService;

    // ─── Test fixtures ────────────────────────────────────────────────────────

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "ahmet";

    private User buildUser(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .build();
    }

    // ─── searchUsers Tests ────────────────────────────────────────────────────

    @Nested
    @DisplayName("searchUsers")
    class SearchUsers {

        @Test
        @DisplayName("returns mapped list of UserResponse when users match the search query")
        void searchUsers_usersFound_returnsResponseList() {
            // given
            String searchQuery = "ahm";
            User user1 = buildUser(1L, "ahmet");
            User user2 = buildUser(2L, "ahmetcan");

            given(userRepository.findByUsernameContainingIgnoreCase(searchQuery))
                    .willReturn(List.of(user1, user2));

            // when
            List<UserResponse> result = userService.searchUsers(searchQuery);

            // then
            assertThat(result).hasSize(2);

            // first user check
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(0).getUsername()).isEqualTo("ahmet");

            // second user check
            assertThat(result.get(1).getId()).isEqualTo(2L);
            assertThat(result.get(1).getUsername()).isEqualTo("ahmetcan");

            then(userRepository).should().findByUsernameContainingIgnoreCase(searchQuery);
        }

        @Test
        @DisplayName("returns empty list when no users match the search query")
        void searchUsers_noUsersFound_returnsEmptyList() {
            // given
            String searchQuery = "nonexistuser";
            given(userRepository.findByUsernameContainingIgnoreCase(searchQuery))
                    .willReturn(Collections.emptyList());

            // when
            List<UserResponse> result = userService.searchUsers(searchQuery);

            // then
            assertThat(result).isEmpty();
            then(userRepository).should().findByUsernameContainingIgnoreCase(searchQuery);
        }
    }

    // ─── getUserById Tests ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("returns UserResponse when user exists")
        void getUserById_userExists_returnsResponse() {
            // given
            User user = buildUser(USER_ID, USERNAME);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            UserResponse result = userService.getUserById(USER_ID);

            // then
            assertThat(result.getId()).isEqualTo(USER_ID);
            assertThat(result.getUsername()).isEqualTo(USERNAME);

            then(userRepository).should().findById(USER_ID);
        }

        @Test
        @DisplayName("throws BusinessException when user does not exist")
        void getUserById_userNotFound_throwsException() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> userService.getUserById(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(MessageType.USER_NOT_FOUND.getMessage());

            then(userRepository).should().findById(USER_ID);
        }
    }
}
