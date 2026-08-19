package com.ditto.aicourse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 장소 카드에 걸 사진 한 장.
 *
 * <p>주소만 필요하면 {@link RecommendedPlaceResponse#getImageUrl()} 을 쓰면 된다.
 * 이 객체는 출처 표기와 사진 종류가 필요할 때 쓴다.
 */
@Getter
@Builder
@AllArgsConstructor
public class PlaceImageResponse {

    @Schema(description = """
            사진의 성격. 화면에서 이 값에 따라 다르게 걸어야 한다.

            - `place` — 그 매장 자체의 사진. 매장 이미지로 그대로 쓰면 된다.
            - `evidence` — 셀럽이 그 브랜드를 착용한 보도사진. **매장 사진이 아니다.** \
            매장 외관인 것처럼 걸면 안 되고, caption 과 source 를 함께 노출해야 한다.
            """,
            allowableValues = {"place", "evidence"},
            example = "evidence")
    private final String kind;

    @Schema(description = "사진 주소. 그대로 <img src> 에 넣을 수 있다.",
            example = "https://hdf-ditto-images.s3.ap-northeast-2.amazonaws.com/place-picture/35_%ED%94%84%EB%9D%BC%EB%8B%A4.jpg")
    private final String url;

    @Schema(description = "출처. evidence 사진은 보도사진이라 이 값을 화면에 표기해야 한다.",
            example = "cdn.straightnews.co.kr")
    private final String source;

    @Schema(description = "사진 설명", example = "카리나 × Prada")
    private final String caption;
}
