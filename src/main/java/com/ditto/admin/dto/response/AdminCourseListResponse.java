package com.ditto.admin.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 승인 대기 코스 초안 <b>목록</b> 응답.
 *
 * <p>목록은 머리말만 온다 — 초안 하나가 조사 원문까지 들고 있어 수십 KB 라, 열 명이면
 * 응답이 메가 단위가 된다. 전문은 상세 조회에서 가져온다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "승인 대기 코스 초안 목록")
public class AdminCourseListResponse {

    @Schema(description = "초안을 만든 람다 이름", example = "ditto-celeb-warm-2")
    private String functionName;

    @Schema(description = "람다를 부른 시각. 이 응답은 캐시가 아니라 요청마다 새로 부른 것이다")
    private Instant fetchedAt;

    @Schema(description = "살아 있는 초안 수", example = "2")
    private int count;

    @Schema(description = "람다 원문. {\"count\":2,\"drafts\":[{\"celebrity\":\"카리나\","
            + "\"kind\":\"PERSON\",\"status\":\"ok\",\"shape\":\"매장 3 · 카페 1 · 여가 1\","
            + "\"places\":5,\"warnings\":2,\"built_at\":\"2026-08-25T14:15:10\",\"ttl\":85785}]}")
    private JsonNode payload;
}
