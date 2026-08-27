package com.ditto.admin.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <b>지금 손님에게 나가고 있는</b> 코스 목록 응답.
 *
 * <p>{@link AdminCourseListResponse} 와 짝이다 — 저쪽이 승인 대기 중인 것이고 이쪽이
 * 승인이 끝나 서비스 중인 것이다. 승인이 초안을 지우므로 한 인물이 양쪽에 동시에
 * 있는 일은 없다.
 *
 * <p>목록은 머리말만 온다. 코스 하나가 세션 상태까지 들고 있어 수백 KB 라, 전문을 담으면
 * 응답이 메가 단위가 된다. 대신 대표 사진(<b>{@code hero}</b>)이 머리말에 실려 있어
 * 화면이 카드를 그리려고 상세를 따로 받을 일이 없다.
 *
 * <p>전부 다음 00시(KST)에 만료된다. {@code ttl} 이 그때까지 남은 초다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "서비스 중인(캐시된) 코스 목록")
public class AdminCourseCacheListResponse {

    @Schema(description = "목록을 읽어 온 람다 이름", example = "ditto-celeb-approve")
    private String functionName;

    @Schema(description = "람다를 부른 시각. 이 응답은 캐시가 아니라 요청마다 새로 부른 것이다")
    private Instant fetchedAt;

    @Schema(description = "서비스 중인 코스 수", example = "3")
    private int count;

    @Schema(description = "람다 원문. {\"count\":1,\"courses\":[{\"celebrity\":\"카리나\","
            + "\"aspect\":\"BRAND\",\"shape\":\"매장 3 · 카페 1 · 여가 1\",\"places\":5,"
            + "\"warnings\":2,\"reply\":\"카리나의 브랜드 선호도를 반영한 코스입니다.\","
            + "\"approved_at\":\"2026-08-26T09:20:11\",\"built_at\":\"2026-08-26T00:12:00\","
            + "\"hero\":{\"url\":\"https://…\",\"kind\":\"evidence\",\"caption\":\"카리나 × 프라다\"},"
            + "\"aliases\":[\"KARINA\",\"카리나\"],\"research\":true,\"ttl\":46853}]}")
    private JsonNode payload;
}
