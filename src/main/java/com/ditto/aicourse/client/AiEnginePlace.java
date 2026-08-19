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
 *  "image": {"kind": "evidence", "url": "...", "source": "...", "caption": "카리나 × Prada"}}
 * </pre>
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

    /**
     * 평평한 사진 주소. 엔진 빌드에 따라 {@code image} 와 함께 오기도 하고 안 오기도 한다.
     * 어느 쪽이 오든 {@link #resolveImageUrl()} 하나로 읽는다.
     */
    @JsonProperty("image_url")
    private String imageUrl;

    /**
     * 쓸 수 있는 사진 주소 하나를 고른다. 없으면 {@code null}.
     *
     * <p>엔진이 평평한 {@code image_url} 을 실어 보내면 그것을, 아니면 {@code image.url} 을 쓴다.
     */
    public String resolveImageUrl() {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl;
        }
        return image == null ? null : image.getUrl();
    }
}
