package com.ditto.navigation.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 실내 지도 원장이 놓인 곳. 파일 자체가 아니라 <b>주소</b>를 돌려준다. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "실내 지도 원장 자산 위치")
public class MapAssetResponse {

    @Schema(description = "원장이 놓인 기준 주소. 비어 있으면 프론트가 자기 사본을 쓴다",
            example = "https://d1bxld598du04o.cloudfront.net/course-resource/navigation/v2")
    private String baseUrl;

    @Schema(description = "CDN 을 쓰고 있나. 거짓이면 프론트가 public/ 사본으로 떨어진다")
    private boolean cdn;

    @Schema(description = "원장의 색인", example = "https://…/manifest.json")
    private String manifestUrl;

    @Schema(description = "길찾기 장소 원장(147곳)", example = "https://…/store-navigation-keys.json")
    private String storeKeysUrl;

    @Schema(description = "층별 방 폴리곤", example = "https://…/floor-rooms.json")
    private String roomsUrl;

    @Schema(description = "층 그래프. 순서가 프론트의 FLOOR_ORDER 와 같다")
    private List<MapFloorAsset> floors;

    @Schema(description = "브라우저가 들고 있어도 되는 시간(초). 기본 3주", example = "1814400")
    private long maxAgeSeconds;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "층 하나의 그래프 파일")
    public static class MapFloorAsset {

        @Schema(description = "층 코드", example = "B2")
        private String floorId;

        @Schema(description = "그 층 그래프 주소", example = "https://…/b2.json")
        private String url;
    }
}
