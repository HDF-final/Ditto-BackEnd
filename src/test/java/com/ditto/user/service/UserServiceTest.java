package com.ditto.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.user.dto.request.UpdatePersonaRequest;
import com.ditto.user.dto.request.UpdateUserProfileRequest;
import com.ditto.user.dto.response.PersonaResponse;
import com.ditto.user.dto.response.UserProfileResponse;
import com.ditto.user.repository.UpdateUserCommand;
import com.ditto.user.repository.UserMapper;
import com.ditto.user.repository.UserRow;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("내 프로필 정보를 정상 조회한다")
    void getMyProfileSuccess() {
        UserRow user = UserRow.builder()
                .userId(USER_ID)
                .countryId(1L)
                .name("사토 유키")
                .email("yuki@example.com")
                .preferredLanguageCode("ja")
                .role("ROLE_CUSTOMER")
                .persona("OPEN_RUN_LOVER")
                .build();

        given(userMapper.findActiveById(USER_ID)).willReturn(Optional.of(user));

        UserProfileResponse response = userService.getMyProfile(USER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getEmail()).isEqualTo("yuki@example.com");
        assertThat(response.getNickname()).isEqualTo("사토 유키");
        assertThat(response.getPersona()).isEqualTo("OPEN_RUN_LOVER");
        assertThat(response.getPersonaDisplayName()).isEqualTo("오픈런러버");
    }

    @Test
    @DisplayName("내 프로필 정보(닉네임, 비밀번호, 페르소나)를 정상 수정한다")
    void updateProfileSuccess() {
        UserRow existingUser = UserRow.builder()
                .userId(USER_ID)
                .countryId(1L)
                .name("사토 유키")
                .email("yuki@example.com")
                .preferredLanguageCode("ja")
                .role("ROLE_CUSTOMER")
                .persona("OPEN_RUN_LOVER")
                .build();

        UserRow updatedUser = UserRow.builder()
                .userId(USER_ID)
                .countryId(1L)
                .name("새로운닉네임")
                .email("yuki@example.com")
                .preferredLanguageCode("ja")
                .role("ROLE_CUSTOMER")
                .persona("FLEX_SPENDER")
                .build();

        given(userMapper.findActiveById(USER_ID))
                .willReturn(Optional.of(existingUser))
                .willReturn(Optional.of(updatedUser));
        given(passwordEncoder.encode("newPassword123!")).willReturn("encodedPasswordHash");
        given(userMapper.updateProfile(any(UpdateUserCommand.class))).willReturn(1);

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .nickname("새로운닉네임")
                .password("newPassword123!")
                .persona("FLEX_SPENDER")
                .build();

        UserProfileResponse response = userService.updateProfile(USER_ID, request);

        assertThat(response).isNotNull();
        assertThat(response.getNickname()).isEqualTo("새로운닉네임");
        assertThat(response.getPersona()).isEqualTo("FLEX_SPENDER");
        assertThat(response.getPersonaDisplayName()).isEqualTo("플렉스족");

        verify(userMapper).updateProfile(any(UpdateUserCommand.class));
    }

    @Test
    @DisplayName("사용자의 페르소나를 정상 조회한다")
    void getPersonaSuccess() {
        UserRow user = UserRow.builder()
                .userId(USER_ID)
                .name("사토 유키")
                .email("yuki@example.com")
                .persona("OPEN_RUN_LOVER")
                .build();

        given(userMapper.findActiveById(USER_ID)).willReturn(Optional.of(user));

        PersonaResponse response = userService.getPersona(USER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getPersona()).isEqualTo("OPEN_RUN_LOVER");
        assertThat(response.getDisplayName()).isEqualTo("오픈런러버");
    }

    @Test
    @DisplayName("사용자의 페르소나를 정상 수정한다")
    void updatePersonaSuccess() {
        UserRow user = UserRow.builder()
                .userId(USER_ID)
                .name("사토 유키")
                .email("yuki@example.com")
                .persona("OPEN_RUN_LOVER")
                .build();

        given(userMapper.findActiveById(USER_ID)).willReturn(Optional.of(user));
        given(userMapper.updatePersona(USER_ID, "FLEX_SPENDER")).willReturn(1);

        UpdatePersonaRequest request = UpdatePersonaRequest.builder()
                .persona("FLEX_SPENDER")
                .build();

        PersonaResponse response = userService.updatePersona(USER_ID, request);

        assertThat(response).isNotNull();
        assertThat(response.getPersona()).isEqualTo("FLEX_SPENDER");
        assertThat(response.getDisplayName()).isEqualTo("플렉스족");

        verify(userMapper).updatePersona(USER_ID, "FLEX_SPENDER");
    }

    @Test
    @DisplayName("유효하지 않은 페르소나 값 입력 시 INVALID_INPUT_VALUE 예외가 발생한다")
    void rejectUpdatePersonaWhenInvalidValue() {
        UserRow user = UserRow.builder()
                .userId(USER_ID)
                .name("사토 유키")
                .email("yuki@example.com")
                .build();

        given(userMapper.findActiveById(USER_ID)).willReturn(Optional.of(user));

        UpdatePersonaRequest request = UpdatePersonaRequest.builder()
                .persona("INVALID_TYPE")
                .build();

        assertThatThrownBy(() -> userService.updatePersona(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
