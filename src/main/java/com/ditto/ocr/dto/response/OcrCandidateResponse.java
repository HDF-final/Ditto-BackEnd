package com.ditto.ocr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인식된 브랜드명과 매칭된 장소 후보.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrCandidateResponse {

    @Schema(description = "장소 ID", example = "11")
    private Long placeId;

    @Schema(description = "길찾기 식별자", example = "3F_STORE_0035")
    private String navigationKey;

    @Schema(description = "장소명", example = "시스템")
    private String name;

    @Schema(description = "층", example = "3F")
    private String floor;

    @Schema(description = "OCR 엔진이 해당 글자를 읽은 신뢰도(0~1). 장소 매칭 점수가 아니다.", example = "0.94")
    private Double confidence;

    @Schema(description = "카탈로그 엔티티 매칭 점수(0~1). exact·alias 는 1.0, OCR 오타는 fuzzy 유사도.", example = "0.91")
    private Double matchScore;
}
