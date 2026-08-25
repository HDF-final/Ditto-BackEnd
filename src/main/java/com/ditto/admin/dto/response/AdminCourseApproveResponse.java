package com.ditto.admin.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 코스 승인 결과.
 *
 * <p>{@code payload} 가 람다 응답 원문이고 나머지는 화면이 바로 쓰는 머리말이다.
 * {@code AdminCourseDetailResponse} 가 상태를 위로 끌어올리는 것과 같은 판단이다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "코스 승인 결과")
public class AdminCourseApproveResponse {

    @Schema(description = "승인을 처리한 람다 이름", example = "ditto-celeb-approve")
    private String functionName;

    @Schema(description = "람다를 부른 시각")
    private Instant approvedAt;

    @Schema(description = "인물 이름", example = "카리나")
    private String celebrity;

    @Schema(description = "손님 캐시에 올라간 자리 수", example = "5")
    private int placeCount;

    @Schema(description = "캐시가 사라지는 시각 — 다음 00시 (KST)",
            example = "2026-08-26T00:00:00")
    private String expiresAt;

    @Schema(description = "관리자가 봐야 할 것. 올라가긴 했으나 원장 반영이 일부 빠진 경우 등")
    private JsonNode warnings;

    @Schema(description = "람다 응답 원문 — wrote(무엇을 썼나)와 oracle(원장 반영)이 들어 있다")
    private JsonNode payload;
}
