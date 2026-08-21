package com.ditto.aicourse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 이 턴에 엔진이 쓴 자원. 코스 내용이 아니라 운영·시험용 수치다.
 *
 * <p>따로 묶어 둔 이유 — 화면이 쓰는 값이 아니라서 코스 본문과 섞이면 안 되고,
 * 시험이 끝나면 이 덩어리만 떼면 되게 하려는 것이다.
 *
 * <p>엔진이 값을 안 주면 각 필드는 null 이다.
 */
@Getter
@Builder
@AllArgsConstructor
public class CourseMetricsResponse {

    @Schema(description = "엔진이 이 턴에 쓴 시간(초). 읽기 타임아웃이 120초라 "
            + "이 값이 거기 가까워지면 손봐야 한다.",
            example = "15.0")
    private final Double seconds;

    @Schema(description = "이 턴에 부른 LLM 횟수", example = "5")
    private final Integer llmCalls;

    @Schema(description = "입력 토큰 수. 첫 턴은 셀럽 조사가 붙어 크고, "
            + "이어지는 턴은 조사 결과를 재사용해 훨씬 작다.",
            example = "31797")
    private final Integer inputTokens;

    @Schema(description = "출력 토큰 수", example = "765")
    private final Integer outputTokens;
}
