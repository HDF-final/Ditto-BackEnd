package com.ditto.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.admin.dto.request.AdminSystemCourseUpdateRequest;
import com.ditto.admin.dto.response.AdminSystemCourseResponse;
import com.ditto.admin.service.AdminSystemCourseService;
import com.ditto.global.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 어드민 "기본 추천 코스" 관리. {@code /api/v1/admin/**} 아래라 SecurityConfig 가
 * ROLE_ADMIN 을 이미 걸어 둔다.
 *
 * <p>{@code /admin-courses} 와 가른 이유: 저쪽은 <b>승인 전후의 셀럽 코스</b>(초안·캐시)를
 * 다루고 하루면 사라지는 것을 본다. 이쪽은 <b>서비스 DB 에 영구히 걸린 코스</b>다.
 * 수명이 다르고, 고칠 수 있는 것도 다르다.
 */
@Tag(name = "Admin - System Courses", description = "관리자 전용 기본 추천 코스 관리 API")
@RestController
@RequestMapping("/api/v1/admin/system-courses")
@RequiredArgsConstructor
public class AdminSystemCourseController {

    private final AdminSystemCourseService adminSystemCourseService;

    @Operation(summary = "기본 추천 코스 목록 조회",
            description = "지금 서비스에 걸려 있는 SYSTEM 코스 전부. 페이지를 안 자른다. "
                    + "반영이 아직 도는 중인 것은 state 가 queued·running 으로 온다.")
    @GetMapping
    public ApiResponse<List<AdminSystemCourseResponse>> getCourses() {
        return ApiResponse.success(adminSystemCourseService.getCourses());
    }

    @Operation(summary = "기본 추천 코스 상세 조회",
            description = "자리 목록과 게시글 본문까지. 수정 화면이 이걸로 폼을 채운다.")
    @GetMapping("/{courseId}")
    public ApiResponse<AdminSystemCourseResponse> getCourse(
            @Parameter(description = "코스 번호", example = "181")
            @PathVariable Long courseId) {
        return ApiResponse.success(adminSystemCourseService.getCourse(courseId));
    }

    @Operation(summary = "기본 추천 코스 수정",
            description = "보낸 칸만 고친다 — 이름·설명·국가코드·게시글 본문·자리별 추천 이유. "
                    + "자리 구성(어느 매장을 몇 번째로)은 셀럽 편집기에서 다시 승인해 덮어쓴다.")
    @PatchMapping("/{courseId}")
    public ApiResponse<AdminSystemCourseResponse> updateCourse(
            @Parameter(description = "코스 번호", example = "181")
            @PathVariable Long courseId,
            @Valid @RequestBody AdminSystemCourseUpdateRequest request) {
        return ApiResponse.success("수정되었습니다.",
                adminSystemCourseService.updateCourse(courseId, request));
    }

    @Operation(summary = "기본 추천 코스 삭제",
            description = "코스와 붙어 있는 게시글을 같이 내린다(soft delete). "
                    + "복사해 간 손님 코스와 AI 즉답 캐시는 안 건드린다.")
    @DeleteMapping("/{courseId}")
    public ApiResponse<Void> deleteCourse(
            @Parameter(description = "코스 번호", example = "181")
            @PathVariable Long courseId) {
        adminSystemCourseService.deleteCourse(courseId);
        return ApiResponse.success("삭제되었습니다.", null);
    }
}
