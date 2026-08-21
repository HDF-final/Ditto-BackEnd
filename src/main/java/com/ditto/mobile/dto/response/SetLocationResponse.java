package com.ditto.mobile.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 현재 위치(경로 시작점) 조회 응답.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetLocationResponse {

    @Schema(description = "현재 위치(시작점) 장소 ID", example = "11")
    private Long placeId;

    @Schema(description = "시작 장소의 길찾기 식별자", example = "F1-A-03")
    private String startNavigationKey;
}
