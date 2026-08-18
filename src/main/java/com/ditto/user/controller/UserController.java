package com.ditto.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.community.service.PostBookmarkService;
import com.ditto.global.common.response.ApiResponse;
import com.ditto.global.common.response.PageResponse;
import com.ditto.security.SecurityUtils;
import com.ditto.user.dto.response.UserBookmarkResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "사용자 마이페이지 및 북마크 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final PostBookmarkService postBookmarkService;

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
}
