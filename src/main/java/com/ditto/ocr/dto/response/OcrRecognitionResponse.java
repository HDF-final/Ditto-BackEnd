package com.ditto.ocr.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OCR 인식 응답.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrRecognitionResponse {

    @Schema(description = "인식 결과 ID", example = "ocr_01")
    private String recognitionId;

    @Schema(description = "간판에서 인식한 브랜드명", example = "TAMBURINS")
    private String recognizedBrandName;

    @Schema(description = "브랜드명과 매칭된 장소 후보 목록")
    private List<OcrCandidateResponse> candidates;
}
