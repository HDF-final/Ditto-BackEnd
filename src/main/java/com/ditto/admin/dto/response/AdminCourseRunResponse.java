package com.ditto.admin.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 오늘 초안 생성 배치가 어디까지 갔나.
 *
 * <p>일꾼 람다가 인물마다 흩어져 돌기 때문에 초안 목록만 봐서는 "아직 도는 중"과
 * "실패해서 영영 안 나올 것"을 구별할 수 없다. 배치는 성공하든 실패하든 마지막에
 * 자기 이름과 사유를 {@code done} 에 적으므로, 관리자가 기다릴지 말지를 여기서 안다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "오늘 코스 초안 생성 실행 상황")
public class AdminCourseRunResponse {

    @Schema(description = "초안을 만든 람다 이름", example = "ditto-celeb-warm-2")
    private String functionName;

    @Schema(description = "람다를 부른 시각. 이 응답은 캐시가 아니라 요청마다 새로 부른 것이다")
    private Instant fetchedAt;

    @Schema(description = "실행 날짜 (람다 기준 KST)", example = "2026-08-25")
    private String date;

    @Schema(description = "아직 시작도 못 한 인물 수. 레인이 비면 하나씩 당겨 간다", example = "0")
    private int queued;

    @Schema(description = "끝난 인물 수. 실패한 인물도 포함한다 — 사유는 payload.done 에 있다",
            example = "2")
    private int doneCount;

    @Schema(description = "람다 원문. {\"date\":\"2026-08-25\",\"queued\":0,"
            + "\"done\":{\"카리나\":\"ok · 매장 3 · 카페 1 · 여가 1\"}}")
    private JsonNode payload;
}
