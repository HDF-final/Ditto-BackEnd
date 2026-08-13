package com.ditto.aicourse.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * AI 코스 추천 한 턴의 결과. 맞춤 생성·대화·재추천이 모두 이 응답을 쓴다.
 */
@Getter
@Builder
@AllArgsConstructor
public class CourseChatResponse {

    @Schema(description = "대화 id. 다음 요청에 그대로 실어 보내면 앞 대화의 조건을 이어받는다. "
            + "재추천도 이 값을 실어 보내면 된다.",
            example = "Op3uskz8Gpo")
    private final String sessionId;

    @Schema(description = "손님에게 보여줄 응답 문장",
            example = "손님께서 요청하신 대로 카리나가 좋아하는 브랜드 구경과 식사를 중심으로 코스를 준비했습니다.")
    private final String reply;

    @Schema(description = "이 대화의 몇 번째 턴인가", example = "1")
    private final Integer turn;

    @Schema(description = "추천된 코스. 방문 순서 그대로이며, 장소마다 navigationKey 와 reason 이 담긴다. "
            + "조건에 맞는 장소를 못 찾으면 빈 배열이 온다.")
    private final List<RecommendedPlaceResponse> places;
}
