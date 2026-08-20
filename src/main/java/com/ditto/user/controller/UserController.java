package com.ditto.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.community.service.PostBookmarkService;
import com.ditto.global.common.response.ApiResponse;
import com.ditto.global.common.response.PageResponse;
import com.ditto.security.SecurityUtils;
import com.ditto.user.dto.request.UpdatePersonaRequest;
import com.ditto.user.dto.request.UpdateUserPreferencesRequest;
import com.ditto.user.dto.request.UpdateUserProfileRequest;
import com.ditto.user.dto.response.PersonaResponse;
import com.ditto.user.dto.response.UserBookmarkResponse;
import com.ditto.user.dto.response.UserPreferencesResponse;
import com.ditto.user.dto.response.UserProfileResponse;
import com.ditto.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "사용자 프로필, 마이페이지, 페르소나 및 북마크 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final PostBookmarkService postBookmarkService;
    private final UserService userService;

    @Operation(
            summary = "내 프로필 정보 조회",
            description = "로그인한 사용자(ROLE_CUSTOMER)의 프로필 정보(이메일, 닉네임, 국가, 언어, 페르소나 등)를 조회합니다.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UserProfileResponse> getMyProfile() {
        return ApiResponse.success("성공",
                userService.getMyProfile(SecurityUtils.requireUserId()));
    }

    @Operation(
            summary = "내 프로필 정보 수정 (닉네임, 비밀번호, 페르소나)",
            description = "로그인한 사용자의 닉네임, 비밀번호, 쇼핑 페르소나를 선택적으로 수정합니다.")
    @PatchMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success("성공",
                userService.updateProfile(SecurityUtils.requireUserId(), request));
    }

    @Operation(
            summary = "국가·언어 환경설정 변경",
            description = "콘텐츠 대상 국가와 표시 언어를 서로 독립적으로 변경합니다.")
    @PatchMapping("/preferences")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UserPreferencesResponse> updateMyPreferences(
            @Valid @RequestBody UpdateUserPreferencesRequest request) {
        return ApiResponse.success("성공",
                userService.updatePreferences(SecurityUtils.requireUserId(), request));
    }

    @Operation(
            summary = "내 북마크 목록 조회",
            description = "로그인한 고객(ROLE_CUSTOMER)이 북마크(저장)한 코스 게시글 목록을 최신순으로 페이징 조회합니다.")
    @GetMapping("/bookmarks")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<UserBookmarkResponse>> getMyBookmarks(
            @Parameter(description = "페이지 번호(0부터 시작, 기본 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기(1~100, 기본 10)", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("성공",
                postBookmarkService.getMyBookmarks(SecurityUtils.requireUserId(), page, size));
    }

    @Operation(
            summary = "쇼핑 페르소나 조회",
            description = "로그인한 사용자의 쇼핑 페르소나 정보를 조회합니다.")
    @GetMapping("/persona")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PersonaResponse> getPersona() {
        return ApiResponse.success("성공",
                userService.getPersona(SecurityUtils.requireUserId()));
    }

    @Operation(
            summary = "쇼핑 페르소나 설정 및 수정",
            description = "온보딩 또는 마이페이지에서 사용자의 쇼핑 페르소나를 설정/수정합니다.")
    @PatchMapping("/persona")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PersonaResponse> updatePersona(
            @Valid @RequestBody UpdatePersonaRequest request) {
        return ApiResponse.success("성공",
                userService.updatePersona(SecurityUtils.requireUserId(), request));
    }
}
