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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ditto.country.repository.CountryMapper;
import com.ditto.country.repository.CountryRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.user.dto.request.UpdatePersonaRequest;
import com.ditto.user.dto.request.UpdateUserPreferencesRequest;
import com.ditto.user.dto.request.UpdateUserProfileRequest;
import com.ditto.user.dto.response.PersonaResponse;
import com.ditto.user.dto.response.UserPreferencesResponse;
import com.ditto.user.dto.response.UserProfileResponse;
import com.ditto.user.repository.UpdateUserPreferencesCommand;
import com.ditto.user.repository.UpdateUserCommand;
import com.ditto.user.repository.UserMapper;
import com.ditto.user.repository.UserRow;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CountryMapper countryMapper;

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
                .countryCode("JP")
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
        assertThat(response.getCountryCode()).isEqualTo("JP");
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
    @DisplayName("국가와 언어를 서로 다른 조합으로 정상 저장한다")
    void updatePreferencesSuccess() {
        UserRow user = UserRow.builder()
                .userId(USER_ID)
                .countryId(1L)
                .countryCode("KR")
                .preferredLanguageCode("ko")
                .build();
        CountryRow japan = CountryRow.builder()
                .countryId(3L)
                .code("JP")
                .defaultLanguageCode("ja")
                .build();

        given(userMapper.findActiveById(USER_ID)).willReturn(Optional.of(user));
        given(countryMapper.findActiveByCode("JP")).willReturn(Optional.of(japan));
        given(userMapper.updatePreferences(any(UpdateUserPreferencesCommand.class))).willReturn(1);

        UserPreferencesResponse response = userService.updatePreferences(
                USER_ID,
                UpdateUserPreferencesRequest.builder()
                        .countryCode("jp")
                        .languageCode("EN")
                        .build());

        assertThat(response.getCountryCode()).isEqualTo("JP");
        assertThat(response.getLanguageCode()).isEqualTo("en");
        ArgumentCaptor<UpdateUserPreferencesCommand> captor =
                ArgumentCaptor.forClass(UpdateUserPreferencesCommand.class);
        verify(userMapper).updatePreferences(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getCountryId()).isEqualTo(3L);
        assertThat(captor.getValue().getLanguageCode()).isEqualTo("en");
    }

    @Test
    @DisplayName("비활성 또는 미지원 국가는 U003으로 거절한다")
    void rejectPreferencesWhenCountryIsUnsupported() {
        given(userMapper.findActiveById(USER_ID)).willReturn(Optional.of(UserRow.builder().userId(USER_ID).build()));

        assertThatThrownBy(() -> userService.updatePreferences(
                USER_ID,
                UpdateUserPreferencesRequest.builder()
                        .countryCode("FR")
                        .languageCode("en")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COUNTRY_CODE);
    }

    @Test
    @DisplayName("지원 국가가 비활성 상태이면 U003으로 거절한다")
    void rejectPreferencesWhenCountryIsInactive() {
        given(userMapper.findActiveById(USER_ID)).willReturn(Optional.of(UserRow.builder().userId(USER_ID).build()));
        given(countryMapper.findActiveByCode("CN")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updatePreferences(
                USER_ID,
                UpdateUserPreferencesRequest.builder()
                        .countryCode("CN")
                        .languageCode("zh")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COUNTRY_CODE);
    }

    @Test
    @DisplayName("미지원 언어는 U004로 거절한다")
    void rejectPreferencesWhenLanguageIsUnsupported() {
        given(userMapper.findActiveById(USER_ID)).willReturn(Optional.of(UserRow.builder().userId(USER_ID).build()));
        given(countryMapper.findActiveByCode("US")).willReturn(Optional.of(
                CountryRow.builder().countryId(4L).code("US").defaultLanguageCode("en").build()));

        assertThatThrownBy(() -> userService.updatePreferences(
                USER_ID,
                UpdateUserPreferencesRequest.builder()
                        .countryCode("US")
                        .languageCode("fr")
                        .build()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_LANGUAGE_CODE);
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
