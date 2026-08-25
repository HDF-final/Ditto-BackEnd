package com.ditto.admin.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인물 한 명의 코스 초안 <b>상세</b> 응답.
 *
 * <p>{@code payload} 가 초안 원문이고 나머지는 목록 줄에 그대로 쓰는 머리말이다. 화면이
 * 상태 하나를 보려고 수십 KB 를 파고들 필요가 없게 위로 끌어올린 것이며,
 * {@code TrendArtifactResponse} 가 {@code status}·{@code builtAt} 을 올려 두는 것과 같은 판단이다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "승인 대기 코스 초안 상세")
public class AdminCourseDetailResponse {

    @Schema(description = "초안을 만든 람다 이름", example = "ditto-celeb-warm-2")
    private String functionName;

    @Schema(description = "람다를 부른 시각. 이 응답은 캐시가 아니라 요청마다 새로 부른 것이다")
    private Instant fetchedAt;

    @Schema(description = "인물 이름", example = "카리나")
    private String celebrity;

    @Schema(description = "인물 종류 — PERSON 또는 GROUP", example = "PERSON")
    private String kind;

    @Schema(description = "초안 상태 — ok / 조사 빈손 / 시간 부족 / 코스 실패", example = "ok")
    private String status;

    @Schema(description = "코스 모양 요약", example = "매장 3 · 카페 1 · 여가 1")
    private String shape;

    @Schema(description = "초안을 만든 시각 (람다 기준 KST)", example = "2026-08-25T14:15:10")
    private String builtAt;

    @Schema(description = "코스에 담긴 장소 수", example = "5")
    private int placeCount;

    @Schema(description = "관리자가 눈으로 봐야 할 경고 수. 사진 없음·근거 어긋남·코스 모양 등",
            example = "2")
    private int warningCount;

    @Schema(description = "초안 원문. reply·places·warnings 와 함께 승인 람다가 재조사 없이 "
            + "코스를 다시 만들 재료(research)와 다음 턴을 잇는 세션 상태(state)까지 들어 있다")
    private JsonNode payload;
}
