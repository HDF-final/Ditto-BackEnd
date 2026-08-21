package com.ditto.mobile.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 현재 위치(경로 시작점) 설정 요청.
 */
@Getter
@Setter
@NoArgsConstructor
public class SetLocationRequest {

    @Schema(description = "현재 위치로 설정할 장소 ID (OCR 인식 결과 등)", example = "11")
    @NotNull(message = "장소 ID는 필수입니다.")
    private Long placeId;
}
