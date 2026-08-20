package com.ditto.aicourse.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 엔진이 돌려주는 장소 한 곳. 엔진은 snake_case 로 응답한다.
 *
 * <pre>
 * {"place_name": "프라다", "navigation_key": "1F_STORE_0035", "reason": "...",
 *  "url": "https://...", "image_url": "https://...",
 *  "image": {"kind","url","source","caption","article","width","height"}}
 * </pre>
 *
 * <p>{@code url} · {@code image_url} · {@code image.url} 은 셋 다 같은 값이다.
 * 엔진이 중간 계층에서 중첩 객체가 누락되는 사고를 겪고 평평한 필드를 덧붙인 것이라,
 * 어느 빌드에 붙였느냐에 따라 오는 조합이 다르다. 그래서 읽는 쪽은
 * {@link #resolveImageUrl()} 하나로 통일한다.
 *
 * <p>엔진이 필드를 더 붙여도 깨지지 않도록 모르는 키는 무시한다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiEnginePlace {

    @JsonProperty("navigation_key")
    private String navigationKey;

    @JsonProperty("place_name")
    private String placeName;

    @JsonProperty("reason")
    private String reason;

    /**
     * 사진. 엔진이 근거 사진을 못 찾으면 매장 사진으로 채우고(124곳 전부 있다),
     * Oracle 조회까지 실패한 경우에만 null 이다.
     */
    @JsonProperty("image")
    private AiEngineImage image;

    /** 평평한 사진 주소. {@code image.url} 과 같은 값. */
    @JsonProperty("image_url")
    private String imageUrl;

    /** 평평한 사진 주소(엔진의 정식 계약 이름). {@code image.url} 과 같은 값. */
    @JsonProperty("url")
    private String url;

    /**
     * 쓸 수 있는 사진 주소 하나를 고른다. 없으면 {@code null}.
     *
     * <p>세 자리 중 채워진 것을 쓴다. 값이 다를 일은 없고, 빌드마다 붙는 자리만 다르다.
     */
    public String resolveImageUrl() {
        if (isUsable(imageUrl)) {
            return imageUrl;
        }
        if (isUsable(url)) {
            return url;
        }
        return image == null || !isUsable(image.getUrl()) ? null : image.getUrl();
    }

    private static boolean isUsable(String value) {
        return value != null && !value.isBlank();
    }
}
