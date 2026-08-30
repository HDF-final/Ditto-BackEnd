package com.ditto.recommendation.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.global.common.response.ApiResponse;
import com.ditto.global.common.response.PageResponse;
import com.ditto.global.i18n.AcceptLanguageResolver;
import com.ditto.recommendation.dto.response.RecommendedCourseResponse;
import com.ditto.recommendation.service.RecommendedCourseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 기본 추천 코스 목록. 로그인 없이 열린다 — 메인에 거는 콘텐츠다.
 */
@Tag(name = "Courses - Recommended", description = "기본 추천 코스(SYSTEM) 목록 API")
@RestController
@RequestMapping("/api/v1/courses/recommended")
@RequiredArgsConstructor
public class RecommendedCourseController {

    private final RecommendedCourseService recommendedCourseService;

    @Operation(summary = "기본 추천 코스 목록 조회",
            description = "creation_type 이 SYSTEM 인 코스를 최신순으로 돌려준다. "
                    + "커뮤니티 목록에는 이 코스들이 안 나온다.")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<RecommendedCourseResponse>> getRecommended(
            @Parameter(description = "페이지 번호(0부터 시작, 기본 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기(1~100, 기본 20)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "국가 코드(KR·JP·CN·US). 비우면 전부", example = "KR")
            @RequestParam(required = false) String country,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false)
            String acceptLanguage) {
        return ApiResponse.success("성공",
                recommendedCourseService.getRecommended(
                        page,
                        size,
                        country,
                        AcceptLanguageResolver.resolve(acceptLanguage)));
    }
}
