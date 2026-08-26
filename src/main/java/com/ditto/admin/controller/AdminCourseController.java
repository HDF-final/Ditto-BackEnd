package com.ditto.admin.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.admin.dto.response.AdminCourseApproveResponse;
import com.ditto.admin.dto.response.AdminCourseCacheListResponse;
import com.ditto.admin.dto.response.AdminCourseDetailResponse;
import com.ditto.admin.dto.response.AdminCourseListResponse;
import com.ditto.admin.dto.response.AdminCoursePlaceCatalogResponse;
import com.ditto.admin.dto.response.AdminCourseRevokeResponse;
import com.ditto.admin.dto.response.AdminCourseRunResponse;
import com.ditto.admin.service.AdminCourseService;
import com.ditto.global.common.response.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;

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
     * 인물 이름이 {@code run} 이나 {@code places} 여도 이 매핑들이 이긴다 — Spring 6 의 경로
     * 매칭은 선언 순서가 아니라 더 구체적인 패턴을 고르고, 고정 문자열이 변수보다 구체적이다.
     * 읽는 사람을 위해 순서도 맞춰 둔다.
     */
    @Operation(summary = "오늘 초안 생성 실행 상황 조회",
            description = "배치가 아직 도는 중인지, 실패로 끝났는지를 본다. 초안 목록만으로는 둘을 구별할 수 없다.")
    @GetMapping("/run")
    public ApiResponse<AdminCourseRunResponse> getRunStatus() {
        return ApiResponse.success(adminCourseService.getRunStatus());
    }

    @Operation(summary = "서비스 중인(캐시된) 코스 목록 조회",
            description = "승인이 끝나 지금 손님에게 나가고 있는 코스. 머리말만 돌려주고, "
                    + "전부 다음 00시(KST)에 만료된다 — ttl 이 그때까지 남은 초다.")
    @GetMapping("/cached")
    public ApiResponse<AdminCourseCacheListResponse> getCachedCourses() {
        return ApiResponse.success(adminCourseService.getCachedCourses());
    }

    @Operation(summary = "서비스 중인 코스 하나 조회",
            description = "어드민 편집기가 아는 모양으로 되돌려 준다 — 초안 상세와 같은 칸이다. "
                    + "고쳐서 승인 창구에 다시 넣으면 덮어쓴다.")
    @GetMapping("/cached/{celebrity}")
    public ApiResponse<AdminCourseDetailResponse> getCachedCourse(
            @Parameter(description = "인물 이름", example = "카리나")
            @PathVariable String celebrity) {
        return ApiResponse.success(adminCourseService.getCachedCourse(celebrity));
    }

    @Operation(summary = "서비스 중인 코스 내리기",
            description = "인물의 캐시를 통째로 뺀다 — 코스(전 축) · 조사 재료 · 사전 매칭 표기. "
                    + "되돌리는 창구는 없다. 다시 올리려면 배치를 돌려 초안을 새로 만들고 승인한다.")
    @DeleteMapping("/cached/{celebrity}")
    public ApiResponse<AdminCourseRevokeResponse> revokeCachedCourse(
            @Parameter(description = "인물 이름", example = "카리나")
            @PathVariable String celebrity) {
        return ApiResponse.success(adminCourseService.revoke(celebrity));
    }

    @Operation(summary = "더현대 장소 카탈로그 조회",
            description = "관리자가 초안의 자리를 갈아 끼울 때 고를 목록. 초안의 차순위 후보로 모자랄 때 쓴다.")
    @GetMapping("/places")
    public ApiResponse<AdminCoursePlaceCatalogResponse> getPlaces(
            @Parameter(description = "람다가 5분간 들고 있는 목록을 무시하고 다시 조회한다", example = "false")
            @RequestParam(defaultValue = "false") boolean fresh) {
        return ApiResponse.success(adminCourseService.getPlaces(fresh));
    }

    @Operation(summary = "인물 한 명의 코스 초안 조회",
            description = "코스 전문. 장소마다 근거 문장·출처 기사·사진이 붙어 있고, 승인 람다가 쓸 조사 원문도 들어 있다.")
    @GetMapping("/{celebrity}")
    public ApiResponse<AdminCourseDetailResponse> getDraft(
            @Parameter(description = "인물 이름", example = "카리나")
            @PathVariable String celebrity) {
        return ApiResponse.success(adminCourseService.getDraft(celebrity));
    }

    @Operation(summary = "코스 초안 승인",
            description = "관리자가 고친 초안을 손님이 받는 캐시로 올리고 초안을 지운다. "
                    + "캐시는 다음 00시(KST)에 만료된다. 되돌리는 창구는 없다 — "
                    + "잘못 올렸으면 고쳐서 다시 승인한다(덮어쓴다).")
    @PostMapping("/{celebrity}/approve")
    public ApiResponse<AdminCourseApproveResponse> approve(
            @Parameter(description = "인물 이름", example = "카리나")
            @PathVariable String celebrity,
            @RequestBody JsonNode draft) {
        return ApiResponse.success(adminCourseService.approve(celebrity, draft));
    }
}
