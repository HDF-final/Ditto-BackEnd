package com.ditto.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.admin.dto.response.AdminCourseDetailResponse;
import com.ditto.admin.dto.response.AdminCourseListResponse;
import com.ditto.admin.dto.response.AdminCourseRunResponse;
import com.ditto.admin.service.AdminCourseService;
import com.ditto.global.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 승인 대기 코스 초안 조회 API. {@code /api/v1/admin/**} 아래라 SecurityConfig 가
 * ROLE_ADMIN 을 이미 걸어 둔다.
 */
@Tag(name = "Admin - Courses", description = "관리자 전용 승인 대기 코스 초안 조회 API")
@RestController
@RequestMapping("/api/v1/admin/admin-courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    @Operation(summary = "승인 대기 코스 초안 목록 조회",
            description = "살아 있는 초안의 머리말만 돌려준다. 전문은 상세 조회에서 가져온다.")
    @GetMapping
    public ApiResponse<AdminCourseListResponse> getDrafts() {
        return ApiResponse.success(adminCourseService.getDrafts());
    }

    /**
     * 인물 이름이 {@code run} 이어도 이 매핑이 이긴다 — Spring 6 의 경로 매칭은 선언 순서가
     * 아니라 더 구체적인 패턴을 고르고, 고정 문자열이 변수보다 구체적이다. 읽는 사람을 위해
     * 순서도 맞춰 둔다.
     */
    @Operation(summary = "오늘 초안 생성 실행 상황 조회",
            description = "배치가 아직 도는 중인지, 실패로 끝났는지를 본다. 초안 목록만으로는 둘을 구별할 수 없다.")
    @GetMapping("/run")
    public ApiResponse<AdminCourseRunResponse> getRunStatus() {
        return ApiResponse.success(adminCourseService.getRunStatus());
    }

    @Operation(summary = "인물 한 명의 코스 초안 조회",
            description = "코스 전문. 장소마다 근거 문장·출처 기사·사진이 붙어 있고, 승인 람다가 쓸 조사 원문도 들어 있다.")
    @GetMapping("/{celebrity}")
    public ApiResponse<AdminCourseDetailResponse> getDraft(
            @Parameter(description = "인물 이름", example = "카리나")
            @PathVariable String celebrity) {
        return ApiResponse.success(adminCourseService.getDraft(celebrity));
    }
}
