package com.ditto.ocr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OCR 길찾기 세션 시작 응답.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrSessionStartResponse {

    @Schema(description = "발급된 세션 ID. 이후 인식 요청에 실어 보낸다.", example = "session_01")
    private String sessionId;

    @Schema(description = "세션 시작 장소 ID", example = "11")
    private Long currentPlaceId;

    @Schema(description = "시작 장소의 길찾기 식별자", example = "1F-A-11")
    private String startNavigationKey;
}
