package com.ditto.ocr.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OCR 길찾기 세션 시작 요청.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrSessionStartRequest {

    @Schema(description = "길찾기를 시작하는 현재 장소 ID", example = "11")
    @NotNull
    private Long placeId;

    @Schema(description = "세션 진입 경로. OCR 로 시작한 경우 \"OCR\".", example = "OCR")
    @NotBlank
    @Size(max = 20)
    private String source;
}
