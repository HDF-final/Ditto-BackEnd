package com.ditto.admin.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 코스 내리기 응답.
 *
 * <p>인물 하나의 캐시를 통째로 뺀 결과다 — 코스(전 축) · 조사 재료 · 사전 매칭 표기.
 * <b>표기까지 빼는 것이 요점이다.</b> 코스만 지우면 사전 매칭에는 계속 걸려, 손님
 * 문장마다 캐시 미스로 0단계를 헛되이 부른다.
 *
 * <p>원장(Oracle)은 안 건드린다. 거기 근거 행은 자정 만료가 박혀 있고, 무엇을 조사했나의
 * 기록이지 손님 경로가 아니다.
 *
 * <p><b>되돌리는 창구는 없다.</b> 다시 올리려면 배치를 돌려 초안을 새로 만들고 승인한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "코스 내리기 결과")
public class AdminCourseRevokeResponse {

    @Schema(description = "내리기를 수행한 람다 이름", example = "ditto-celeb-approve")
    private String functionName;

    @Schema(description = "내린 시각")
    private Instant revokedAt;

    @Schema(description = "내린 인물", example = "카리나")
    private String celebrity;

    @Schema(description = "지운 키 수 (코스 · 조사 재료)", example = "2")
    private int keys;

    @Schema(description = "뺀 사전 매칭 표기 수", example = "3")
    private int aliases;

    @Schema(description = "람다 원문. {\"revoke\":[\"카리나\"],\"keys\":2,\"aliases\":3}")
    private JsonNode payload;
}
