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

    @Schema(description = "여러 매장이 같은 점수로 잡혀 사용자가 골라야 하는지 여부. "
            + "true 면 candidates 를 선택지로 보여 주고 자동 진행하지 않는다(예: 프라다 vs 프라다뷰티). "
            + "false 면 candidates[0] 이 확정 답이다.", example = "false")
    private boolean requiresSelection;

    @Schema(description = "브랜드명과 매칭된 장소 후보 목록. requiresSelection 이 true 면 사용자가 고를 분기 선택지다.")
    private List<OcrCandidateResponse> candidates;
}
