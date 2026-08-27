package com.ditto.navigation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.navigation.config.MapAssetProperties;
import com.ditto.navigation.dto.response.MapAssetResponse;

class MapAssetServiceTest {

    private MapAssetService service(String baseUrl) {
        MapAssetProperties properties = new MapAssetProperties();
        properties.setBaseUrl(baseUrl);
        return new MapAssetService(properties);
    }

    @Test
    @DisplayName("CDN 주소를 붙여 층별 원장 주소를 만든다")
    void buildsCdnUrls() {
        MapAssetResponse response =
                service("https://d1bxld598du04o.cloudfront.net/course-resource/navigation/v2").getAssets();

        assertThat(response.isCdn()).isTrue();
        assertThat(response.getManifestUrl())
                .isEqualTo("https://d1bxld598du04o.cloudfront.net/course-resource/navigation/v2/manifest.json");
        assertThat(response.getStoreKeysUrl()).endsWith("/store-navigation-keys.json");
        assertThat(response.getRoomsUrl()).endsWith("/floor-rooms.json");
        assertThat(response.getMaxAgeSeconds()).isEqualTo(1_814_400L);
    }

    @Test
    @DisplayName("층 순서와 파일 이름이 프론트의 FLOOR_ORDER 와 같다")
    void keepsFloorOrder() {
        MapAssetResponse response = service("https://cdn.example/base").getAssets();

        assertThat(response.getFloors())
                .extracting(MapAssetResponse.MapFloorAsset::getFloorId)
                .containsExactly("B2", "B1", "1F", "2F", "3F", "4F", "5F", "6F");
        assertThat(response.getFloors().get(0).getUrl()).isEqualTo("https://cdn.example/base/b2.json");
        assertThat(response.getFloors().get(2).getUrl()).isEqualTo("https://cdn.example/base/1f.json");
    }

    @Test
    @DisplayName("끝의 빗금은 지운다 — 주소가 //로 겹치면 CloudFront 가 다른 키로 본다")
    void trimsTrailingSlash() {
        assertThat(service("https://cdn.example/base///").getAssets().getManifestUrl())
                .isEqualTo("https://cdn.example/base/manifest.json");
    }

    @Test
    @DisplayName("기준 주소가 비면 파일 이름만 준다 — 프론트가 자기 사본에 이어 붙인다")
    void fallsBackToFileNames() {
        MapAssetResponse response = service("").getAssets();

        assertThat(response.isCdn()).isFalse();
        assertThat(response.getBaseUrl()).isEmpty();
        assertThat(response.getManifestUrl()).isEqualTo("manifest.json");
        assertThat(response.getFloors())
                .extracting(MapAssetResponse.MapFloorAsset::getUrl)
                .contains("b2.json", "6f.json");
    }

    @Test
    @DisplayName("null 도 빈 값으로 다룬다")
    void treatsNullAsEmpty() {
        assertThat(service(null).getAssets().isCdn()).isFalse();
    }

    @Test
    @DisplayName("층 목록을 바꾸면 그대로 따른다")
    void followsConfiguredFloors() {
        MapAssetProperties properties = new MapAssetProperties();
        properties.setBaseUrl("https://cdn.example");
        properties.setFloors(List.of("1F", "2F"));

        assertThat(new MapAssetService(properties).getAssets().getFloors()).hasSize(2);
    }
}
