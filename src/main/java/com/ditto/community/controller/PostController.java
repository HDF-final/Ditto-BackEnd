package com.ditto.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.community.dto.request.CreateCoursePostRequest;
import com.ditto.community.dto.response.CreateCoursePostResponse;
import com.ditto.community.service.PostService;
import com.ditto.global.common.response.ApiResponse;
import com.ditto.security.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Post", description = "커뮤니티 게시글 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/courses")
public class PostController {

    private final PostService postService;

    @Operation(summary = "코스 게시글 작성", description = "로그인한 사용자가 본인 소유 코스를 커뮤니티 게시글로 공개합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CreateCoursePostResponse> createCoursePost(
            @Valid @RequestBody CreateCoursePostRequest request) {
        return ApiResponse.success("성공",
                postService.createCoursePost(SecurityUtils.requireUserId(), request));
    }
}
