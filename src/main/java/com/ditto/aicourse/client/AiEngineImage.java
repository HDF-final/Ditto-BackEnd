package com.ditto.aicourse.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 엔진이 장소마다 붙여 주는 사진 한 장.
 *
 * <pre>
 * {"kind": "evidence", "url": "https://...", "source": "brunch.co.kr",
 *  "caption": "카리나 × 프라다", "article": "https://brunch.co.kr/@jennafashion/8",
 *  "width": 1080, "height": 1350}
 * </pre>
 *
 * <p>{@code kind} 를 반드시 같이 내보내야 한다 — 두 사진은 성격이 전혀 다르다.
 * {@code place} 는 그 매장 자체의 사진이지만 {@code evidence} 는 셀럽이 그 브랜드를
 * 착용한 보도사진이다. 구분 없이 "매장 사진"으로 걸면 프라다 매장 자리에
 * 카리나 화보가 매장 외관인 것처럼 걸린다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiEngineImage {

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("url")
    private String url;

    @JsonProperty("source")
    private String source;

    @JsonProperty("caption")
    private String caption;

    /** 사진이 실려 있던 기사 주소. evidence 사진에만 붙는다. */
    @JsonProperty("article")
    private String article;

    /** 원본 가로 픽셀. 화면이 자리를 미리 잡을 때 쓴다. */
    @JsonProperty("width")
    private Integer width;

    /** 원본 세로 픽셀 */
    @JsonProperty("height")
    private Integer height;
}
