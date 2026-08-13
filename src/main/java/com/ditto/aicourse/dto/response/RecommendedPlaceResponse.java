package com.ditto.aicourse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 추천 코스에 담긴 장소 한 곳.
 *
 * <p>결과물은 {@code navigationKey} 와 {@code reason} 이다.
 * 코스를 DB 에 저장하지 않으므로 Oracle {@code place_id} 는 싣지 않는다 —
 * 클라이언트는 {@code navigationKey} 로 실내지도에서 장소를 찾는다.
 */
@Getter
@Builder
@AllArgsConstructor
public class RecommendedPlaceResponse {

    @Schema(description = "실내지도 내비게이션 키. 이 값으로 지도에서 장소를 찾는다.",
            example = "1F_STORE_0035")
    private final String navigationKey;

    @Schema(description = "장소명. 화면 표시용이며 지도 조회에는 navigationKey 를 쓴다.",
            example = "프라다")
    private final String placeName;

    @Schema(description = "이 장소를 코스에 넣은 이유",
            example = "카리나가 2024년부터 프라다 앰버서더로 활동하며 평소에도 애정을 보여 "
                    + "럭셔리 브랜드를 첫 코스로 잡았습니다.")
    private final String reason;
}
