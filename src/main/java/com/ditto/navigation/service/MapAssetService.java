package com.ditto.navigation.service;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.ditto.navigation.config.MapAssetProperties;
import com.ditto.navigation.dto.response.MapAssetResponse;
import com.ditto.navigation.dto.response.MapAssetResponse.MapFloorAsset;

import lombok.RequiredArgsConstructor;

/**
 * 실내 지도 원장이 놓인 곳을 알려 준다. <b>파일을 실어 나르지 않는다.</b>
 *
 * <p>588KB 를 EC2 로 통과시키면 CDN 을 둔 이유가 사라진다. 브라우저가 CloudFront 에서
 * 직접 받고, 백엔드는 주소를 한 곳에서 정하는 일만 한다.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(MapAssetProperties.class)
public class MapAssetService {

    private final MapAssetProperties properties;

    public MapAssetResponse getAssets() {
        String base = trimTrailingSlash(properties.getBaseUrl());
        boolean cdn = !base.isEmpty();

        List<MapFloorAsset> floors = properties.getFloors().stream()
                .map(floorId -> MapFloorAsset.builder()
                        .floorId(floorId)
                        .url(url(base, floorId.toLowerCase(Locale.ROOT) + ".json"))
                        .build())
                .toList();

        return MapAssetResponse.builder()
                .baseUrl(base)
                .cdn(cdn)
                .manifestUrl(url(base, properties.getManifestFile()))
                .storeKeysUrl(url(base, properties.getStoreKeysFile()))
                .roomsUrl(url(base, properties.getRoomsFile()))
                .floors(floors)
                .maxAgeSeconds(properties.getMaxAgeSeconds())
                .build();
    }

    /**
     * 기준 주소가 비어 있으면 파일 이름만 돌려준다. 프론트가 자기 {@code public/} 사본에
     * 이어 붙이면 되는 모양이라, CDN 을 안 쓰는 환경에서도 응답이 쓸모 있다.
     */
    private String url(String base, String file) {
        return base.isEmpty() ? file : base + "/" + file;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
