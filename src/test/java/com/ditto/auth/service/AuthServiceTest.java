package com.ditto.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.ditto.auth.dto.request.LoginRequest;
import com.ditto.auth.dto.request.SignupRequest;
import com.ditto.auth.dto.response.AuthUserResponse;
import com.ditto.auth.dto.response.LoginResponse;
import com.ditto.auth.dto.response.SignupResponse;
import com.ditto.country.repository.CountryMapper;
import com.ditto.country.repository.CountryRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.security.AuthUser;
import com.ditto.user.domain.UserStatus;
import com.ditto.user.repository.SignupUserCommand;
import com.ditto.user.repository.UserMapper;
import com.ditto.user.repository.UserRow;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

    @Mock
    private UserMapper userMapper;

    @Mock
    private CountryMapper countryMapper;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(
                userMapper,
                countryMapper,
                passwordEncoder,
                new HttpSessionSecurityContextRepository());
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("정상 이메일·비밀번호로 로그인하면 SecurityContext를 HttpSession에 저장한다")
    void loginStoresSecurityContextInSession() {
        UserRow user = activeUser("yuki@example.com", passwordEncoder.encode("password123!"));
        given(userMapper.findActiveByEmail("yuki@example.com")).willReturn(Optional.of(user));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        LoginResponse result = authService.login(
                LoginRequest.builder()
                        .userEmail("yuki@example.com")
                        .password("password123!")
                        .build(),
                request,
                response);

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getEmail()).isEqualTo("yuki@example.com");
        assertThat(result.getNickname()).isEqualTo("사토 유키");
        assertThat(result.getRole()).isEqualTo("CUSTOMER");

        Object savedContext = request.getSession(false).getAttribute(SECURITY_CONTEXT_KEY);
        assertThat(savedContext).isInstanceOf(SecurityContext.class);
        SecurityContext context = (SecurityContext) savedContext;
        assertThat(context.getAuthentication().isAuthenticated()).isTrue();
        assertThat(context.getAuthentication().getPrincipal()).isInstanceOf(AuthUser.class);
        AuthUser principal = (AuthUser) context.getAuthentication().getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getEmail()).isEqualTo("yuki@example.com");
    }

    @Test
    @DisplayName("잘못된 비밀번호는 로그인 실패로 처리한다")
    void rejectLoginWhenPasswordMismatch() {
        UserRow user = activeUser("yuki@example.com", passwordEncoder.encode("password123!"));
        given(userMapper.findActiveByEmail("yuki@example.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(
                LoginRequest.builder()
                        .userEmail("yuki@example.com")
                        .password("wrong-password")
                        .build(),
                new MockHttpServletRequest(),
                new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_UNAUTHENTICATED);
    }

    @Test
    @DisplayName("존재하지 않는 이메일과 비밀번호 오류는 같은 외부 오류로 반환한다")
    void rejectLoginWithSameErrorWhenEmailNotFound() {
        given(userMapper.findActiveByEmail("unknown@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                LoginRequest.builder()
                        .userEmail("unknown@example.com")
                        .password("password123!")
                        .build(),
                new MockHttpServletRequest(),
                new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.LOGIN_UNAUTHENTICATED);
                    assertThat(businessException.getMessage()).isEqualTo("인증되지 않은 사용자입니다.");
                });
    }

    @Test
    @DisplayName("비활성 사용자는 active 조회 결과가 없으므로 로그인 실패로 처리한다")
    void rejectLoginWhenUserInactive() {
        given(userMapper.findActiveByEmail("inactive@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                LoginRequest.builder()
                        .userEmail("inactive@example.com")
                        .password("password123!")
                        .build(),
                new MockHttpServletRequest(),
                new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_UNAUTHENTICATED);
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 S002로 거절한다")
    void rejectLoginWhenEmailInvalid() {
        assertThatThrownBy(() -> authService.login(
                LoginRequest.builder()
                        .userEmail("invalid-email")
                        .password("password123!")
                        .build(),
                new MockHttpServletRequest(),
                new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_EMAIL_FORMAT);
    }

    @Test
    @DisplayName("비밀번호를 입력하지 않으면 L002로 거절한다")
    void rejectLoginWhenPasswordBlank() {
        assertThatThrownBy(() -> authService.login(
                LoginRequest.builder()
                        .userEmail("yuki@example.com")
                        .password("")
                        .build(),
                new MockHttpServletRequest(),
                new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_PASSWORD_REQUIRED);
    }

    @Test
    @DisplayName("세션 사용자 ID로 내 인증 사용자 정보를 조회한다")
    void getMe() {
        UserRow user = activeUser("yuki@example.com", passwordEncoder.encode("password123!"));
        given(userMapper.findActiveById(1L)).willReturn(Optional.of(user));

        AuthUserResponse response = authService.getMe(1L);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("yuki@example.com");
    }

    @Test
    @DisplayName("세션 사용자 ID가 ACTIVE 사용자에 없으면 UNAUTHORIZED")
    void rejectGetMeWhenUserNotActive() {
        given(userMapper.findActiveById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("로그아웃은 SecurityContext를 비우고 기존 세션과 JSESSIONID 쿠키를 만료한다")
    void logoutInvalidatesSessionAndCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(SECURITY_CONTEXT_KEY, SecurityContextHolder.createEmptyContext());
        MockHttpServletResponse response = new MockHttpServletResponse();

        authService.logout(request, response);

        assertThat(request.getSession(false)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getCookie("JSESSIONID")).isNotNull();
        assertThat(response.getCookie("JSESSIONID").getMaxAge()).isZero();
    }

    @Test
    @DisplayName("회원가입 비밀번호는 BCrypt 해시로 저장한다")
    void signupStoresBcryptPasswordHash() {
        given(userMapper.countByEmail("new@example.com")).willReturn(0);
        given(countryMapper.findActiveByCode("JP"))
                .willReturn(Optional.of(CountryRow.builder()
                        .countryId(2L)
                        .defaultLanguageCode("ja")
                        .build()));

        SignupResponse response = authService.signup(SignupRequest.builder()
                .userEmail("new@example.com")
                .password("password123!")
                .name("사토 유키")
                .countryCode("JP")
                .build());

        ArgumentCaptor<SignupUserCommand> captor = ArgumentCaptor.forClass(SignupUserCommand.class);
        verify(userMapper).insert(captor.capture());
        SignupUserCommand command = captor.getValue();
        assertThat(command.getPasswordHash()).isNotEqualTo("password123!");
        assertThat(passwordEncoder.matches("password123!", command.getPasswordHash())).isTrue();
        assertThat(command.getStatus()).isEqualTo(UserStatus.ACTIVE.name());
        assertThat(response.getUserEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("중복 이메일 회원가입은 insert 하지 않는다")
    void rejectSignupWhenEmailDuplicated() {
        given(userMapper.countByEmail("new@example.com")).willReturn(1);

        assertThatThrownBy(() -> authService.signup(SignupRequest.builder()
                .userEmail("new@example.com")
                .password("password123!")
                .name("사토 유키")
                .countryCode("JP")
                .build()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_SIGNUP_EMAIL);

        verify(userMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    private UserRow activeUser(String email, String passwordHash) {
        return UserRow.builder()
                .userId(1L)
                .name("사토 유키")
                .email(email)
                .passwordHash(passwordHash)
                .preferredLanguageCode("ja")
                .status(UserStatus.ACTIVE.name())
                .build();
    }
}
