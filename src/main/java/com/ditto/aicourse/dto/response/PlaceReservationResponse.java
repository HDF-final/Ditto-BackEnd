package com.ditto.aicourse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PlaceReservationResponse {

    @Schema(description = "예약 제공사 코드", example = "CATCH_TABLE")
    private final String provider;

    @Schema(description = "예약처(식당)명", example = "ETF 베이커리")
    private final String placeName;

    @Schema(description = "예약 버튼 클릭 시 이동할 URL",
            example = "https://www.catchtable.net/shop/etfbakerthehyundai")
    private final String reservationUrl;
}
