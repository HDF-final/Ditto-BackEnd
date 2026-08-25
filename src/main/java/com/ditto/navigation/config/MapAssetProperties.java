package com.ditto.navigation.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code ditto.map-assets.*} — 실내 지도 원장이 놓인 곳.
 *
 * <p>층 그래프·장소 원장·방 폴리곤은 한 번 만들어지면 바뀌지 않는 정적 파일이라 S3 에 두고
 * CloudFront({@code course-resource/*})로 내보낸다. 백엔드는 <b>그 파일을 대신 실어 나르지
 * 않는다</b> — 588KB 를 EC2 로 통과시키면 CDN 을 둔 이유가 사라진다. 대신 <b>어디에 있는지를
 * 알려 준다.</b>
 *
 * <p>주소를 한 곳에서 정하는 것이 요지다. 프론트가 도메인을 박아 두면 배포 환경마다 다른 값이
 * 필요하고, 버킷을 옮길 때 프론트를 다시 빌드해야 한다.
 *
 * <p>{@code base-url} 을 비우면 프론트가 자기 {@code public/} 안의 사본으로 떨어진다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ditto.map-assets")
public class MapAssetProperties {

    /** 원장이 놓인 기준 주소. 끝의 빗금은 있어도 되고 없어도 된다. */
    private String baseUrl = "";

    /** 층 순서. 프론트의 FLOOR_ORDER 와 같아야 한다. */
    private List<String> floors = List.of("B2", "B1", "1F", "2F", "3F", "4F", "5F", "6F");

    private String manifestFile = "manifest.json";

    private String storeKeysFile = "store-navigation-keys.json";

    private String roomsFile = "floor-rooms.json";

    /**
     * 브라우저가 이 파일들을 며칠 들고 있어도 되는지. 기본 3주다 — 안 바뀌는 파일이다.
     *
     * <p>실제 캐시는 S3 오브젝트의 {@code Cache-Control} 이 정한다. 이 값은 화면이 "얼마나
     * 오래된 것을 보고 있나" 를 알려 주기 위한 참고값이다.
     */
    private long maxAgeSeconds = 1_814_400L;
}
