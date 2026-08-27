package com.ditto.admin.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 더현대 장소 전부. 관리자가 초안의 자리를 갈아 끼울 때 고를 재료다.
 *
 * <p>초안에는 자리마다 차순위 후보({@code alternates})가 붙어 있지만 비어 있는 자리가 있고,
 * 관리자가 그 밖에서 고르고 싶을 때가 있다. 그때 관리자 화면이 DB 에 직접 붙게 하면
 * 자격증명이 하나 더 생기고 스키마가 두 곳에 박힌다 — 초안을 만드는 람다가 이미 장소 DB 와
 * 사진 DB 둘 다에 붙어 있으니 거기서 받아 온다.
 *
 * <p>캡션은 없다. 근거 사진의 캡션({@code 카리나 × 프라다})은 셀럽 근거에 묶인 문장이라,
 * 카탈로그에서 아무 매장이나 골랐을 때 따라가면 없는 관계를 단정하게 된다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "더현대 장소 카탈로그 (관리자가 코스 자리를 교체할 때 고를 목록)")
public class AdminCoursePlaceCatalogResponse {

    @Schema(description = "목록을 내준 람다 이름", example = "ditto-celeb-warm-2")
    private String functionName;

    @Schema(description = "람다를 부른 시각")
    private Instant fetchedAt;

    @Schema(description = "장소 수", example = "147")
    private int count;

    @Schema(description = "람다 원문. {\"count\":147,\"places\":[{\"navigation_key\":\"1F_STORE_0031\","
            + "\"place_name\":\"구찌\",\"floor\":\"1F\",\"place_type\":\"상점\",\"category\":\"럭셔리\","
            + "\"price_tier\":\"LUXURY\",\"image_url\":\"https://…\"}]}")
    private JsonNode payload;
}
