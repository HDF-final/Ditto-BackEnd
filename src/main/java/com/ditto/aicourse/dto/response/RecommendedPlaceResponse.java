package com.ditto.aicourse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 추천 코스에 담긴 장소 한 곳.
 *
 * <p>결과물은 {@code navigationKey}, {@code reason}, 그리고 사진이다.
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

    @Schema(description = "장소 종류. 아이콘·필터에 쓰라고 4가지로 묶어서 준다. "
            + "엔진 쪽 세부 카테고리는 23종이지만 화면에 걸기엔 너무 잘아 묶은 값이다.",
            allowableValues = {"매장", "음식점", "카페", "여가"},
            example = "매장")
    private final String category;

    @Schema(description = "이 장소를 코스에 넣은 이유",
            example = "카리나가 2024년부터 프라다 앰버서더로 활동하며 평소에도 애정을 보여 "
                    + "럭셔리 브랜드를 첫 코스로 잡았습니다.")
    private final String reason;

    @Schema(description = "장소 카드에 걸 사진 주소. 주소만 필요하면 이 값만 쓰면 된다. "
            + "사진을 못 구한 드문 경우에만 null 이므로 화면은 그 경우만 대비하면 된다. "
            + "다만 이 주소가 매장 사진인지 셀럽 보도사진인지는 image.kind 를 봐야 안다.",
            example = "https://cdn.straightnews.co.kr/news/photo/202409/253829_158493_3815.jpg")
    private final String imageUrl;

    @Schema(description = "사진의 출처·설명·종류. 출처 표기가 필요하거나 "
            + "매장 사진과 보도사진을 구분해 걸어야 할 때 쓴다.")
    private final PlaceImageResponse image;
}
