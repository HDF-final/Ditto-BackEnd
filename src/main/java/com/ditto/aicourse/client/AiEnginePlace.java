package com.ditto.aicourse.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 엔진이 돌려주는 장소 한 곳. 엔진은 snake_case 로 응답한다.
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
}
