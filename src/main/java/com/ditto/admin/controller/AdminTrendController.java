package com.ditto.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.admin.dto.response.TrendArtifactResponse;
import com.ditto.admin.service.AdminTrendService;
import com.ditto.global.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin - Trends", description = "관리자 전용 트렌드 산출물 조회 API")
@RestController
@RequestMapping("/api/v1/admin/trends")
@RequiredArgsConstructor
public class AdminTrendController {

    private final AdminTrendService adminTrendService;

    @Operation(summary = "국가별 트렌드 TOP 10 조회")
    @GetMapping("/top10")
    public ApiResponse<TrendArtifactResponse> getTop10() {
        return ApiResponse.success(adminTrendService.getTop10());
    }

    @Operation(summary = "국가별 트렌드 TOP 4 호환본 조회")
    @GetMapping("/top4")
    public ApiResponse<TrendArtifactResponse> getTop4() {
        return ApiResponse.success(adminTrendService.getTop4());
    }

    @Operation(summary = "국가별 트렌드 후보군 조회")
    @GetMapping("/candidates")
    public ApiResponse<TrendArtifactResponse> getCandidates() {
        return ApiResponse.success(adminTrendService.getCandidates());
    }

    @Operation(summary = "YouTube 급상승 TOP 10 조회")
    @GetMapping("/youtube")
    public ApiResponse<TrendArtifactResponse> getYoutube() {
        return ApiResponse.success(adminTrendService.getYoutube());
    }
}
