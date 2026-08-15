package com.ditto.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.community.dto.request.CreateCoursePostRequest;
import com.ditto.community.dto.request.UpdateCoursePostRequest;
import com.ditto.community.dto.response.CreateCoursePostResponse;
import com.ditto.community.dto.response.PublicCourseDetailResponse;
import com.ditto.community.dto.response.PublicCourseResponse;
import com.ditto.community.dto.response.UpdateCoursePostResponse;
import com.ditto.community.service.PostService;
import com.ditto.global.common.response.ApiResponse;
import com.ditto.global.common.response.PageResponse;
import com.ditto.security.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Post", description = "커뮤니티 게시글 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/courses")
public class PostController {

    private final PostService postService;

    @Operation(
            summary = "공개 코스 목록 조회",
            description = "커뮤니티에 공개된 코스 게시글 목록을 최신순으로 페이징 조회합니다.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<PublicCourseResponse>> getPublicCourses(
            @Parameter(description = "페이지 번호(0부터 시작, 기본 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기(1~100, 기본 10)", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("성공", postService.getPublicCourses(page, size));
    }

    @Operation(
            summary = "공개 코스 상세 조회",
            description = "커뮤니티에 공개된 코스 게시글의 본문과 연결된 코스 및 장소 목록을 상세 조회합니다.")
    @GetMapping("/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PublicCourseDetailResponse> getPublicCourse(
            @Parameter(description = "조회할 게시글 ID", example = "1")
            @PathVariable Long postId) {
        return ApiResponse.success("성공", postService.getPublicCourse(postId));
    }

    @Operation(summary = "코스 게시글 작성", description = "로그인한 사용자가 본인 소유 코스를 커뮤니티 게시글로 공개합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CreateCoursePostResponse> createCoursePost(
            @Valid @RequestBody CreateCoursePostRequest request) {
        return ApiResponse.success("성공",
                postService.createCoursePost(SecurityUtils.requireUserId(), request));
    }

    @Operation(summary = "코스 게시글 수정", description = "로그인한 사용자가 자신이 작성한 코스 게시글의 제목과 내용을 수정합니다.")
    @PatchMapping("/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UpdateCoursePostResponse> updateCoursePost(
            @PathVariable Long postId,
            @Valid @RequestBody UpdateCoursePostRequest request) {
        return ApiResponse.success("성공",
                postService.updateCoursePost(SecurityUtils.requireUserId(), postId, request));
    }

    @Operation(summary = "코스 게시글 삭제", description = "로그인한 사용자가 자신이 작성한 코스 게시글을 소프트 삭제합니다.")
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> deleteCoursePost(@PathVariable Long postId) {
        postService.deleteCoursePost(SecurityUtils.requireUserId(), postId);
        return ApiResponse.success("성공", null);
    }
}
