package com.ditto.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.auth.dto.request.LoginRequest;
import com.ditto.auth.dto.response.AuthUserResponse;
import com.ditto.auth.dto.response.LoginResponse;
import com.ditto.auth.dto.request.SignupRequest;
import com.ditto.auth.dto.response.SignupResponse;
import com.ditto.auth.service.AuthService;
import com.ditto.global.common.response.ApiResponse;
import com.ditto.security.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일 중복 여부를 확인하고 신규 회원을 생성합니다.")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success("성공", authService.signup(request));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호를 검증하고 JSESSIONID 세션 인증을 생성합니다.")
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        return ApiResponse.success("성공",
                authService.login(request, httpRequest, response));
    }

    @Operation(summary = "로그아웃", description = "현재 세션을 무효화하고 JSESSIONID 쿠키를 만료시킵니다.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ApiResponse.success("로그아웃되었습니다.", null);
    }

    @Operation(summary = "내 인증 사용자 조회", description = "현재 JSESSIONID 세션의 사용자 정보를 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me() {
        return ApiResponse.success("성공", authService.getMe(SecurityUtils.requireUserId()));
    }
}
