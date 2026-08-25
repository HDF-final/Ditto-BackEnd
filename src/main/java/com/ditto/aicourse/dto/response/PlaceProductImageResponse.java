package com.ditto.aicourse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PlaceProductImageResponse {

    @Schema(description = "상품 ID", example = "101")
    private final Long productId;

    @Schema(description = "상품명", example = "New Balance 574")
    private final String productName;

    @Schema(description = "브랜드 ID", example = "12")
    private final Long brandId;

    @Schema(description = "브랜드명", example = "뉴발란스")
    private final String brandName;

    @Schema(description = "화면에 노출할 상품 이미지 URL",
            example = "https://image.example.com/new-balance-574.jpg")
    private final String imageUrl;

    @Schema(description = "이미지 클릭 시 이동할 상품 원문 URL",
            example = "https://www.nbkorea.com/product/...")
    private final String productUrl;
}
