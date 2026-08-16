package com.ditto.news.inbound.rest.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ditto.global.common.response.ApiResponse;
import com.ditto.news.application.service.NewsFeedPipelineService;
import com.ditto.news.inbound.rest.dto.request.NewsPipelineDebugRequest;
import com.ditto.news.inbound.rest.dto.response.NewsPipelineDebugResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 뉴스피드 생성 파이프라인 수동 테스트 및 데이터 디버깅 컨트롤러.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/news/debug")
@RequiredArgsConstructor
@Tag(name = "News Feed Debug", description = "뉴스피드 생성 파이프라인 수동 실행 및 데이터 디버깅 API")
public class NewsFeedDebugController {

    private final NewsFeedPipelineService pipelineService;

    @PostMapping("/pipeline")
    @Operation(summary = "뉴스피드 파이프라인 수동 실행 및 결과 디버깅",
               description = "지정된 토픽(기본 K-POP)에 대해 RSS 후보 수집 -> Python 본문 크롤링 -> 가중치 선별 -> 피드 생성을 즉시 실행하고 단계별 상세 결과를 반환합니다.")
    public ApiResponse<NewsPipelineDebugResponse> runPipelineDebug(
            @Valid @RequestBody(required = false) NewsPipelineDebugRequest request) {

        String topic = (request != null && request.getTopic() != null && !request.getTopic().isBlank())
                ? request.getTopic().trim()
                : "K-POP";

        log.info("📢 [수동 디버깅 API 호출] 토픽: {}", topic);
        NewsPipelineDebugResponse response = pipelineService.executePipelineWithDebug(topic);

        return ApiResponse.success(response);
    }
}
