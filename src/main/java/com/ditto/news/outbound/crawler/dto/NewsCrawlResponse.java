package com.ditto.news.outbound.crawler.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Python 뉴스 크롤러 서비스 응답 DTO.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsCrawlResponse {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    /** 기사 제목 */
    @JsonProperty("title")
    private String title;

    /** 기사 본문 */
    @JsonProperty("body")
    private String body;

    /** 기사 URL */
    @JsonProperty("url")
    private String url;

    /** 언론사 / 출처 */
    @JsonProperty("source")
    private String source;

    /** 기사 발행 일시 (ISO-8601 with Offset, 예: 2026-08-16T11:11:32+09:00) */
    @JsonProperty("published_at")
    private OffsetDateTime publishedAt;

    /** 대표 이미지 URL */
    @JsonProperty("image_url")
    private String imageUrl;

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getUrl() {
        return url;
    }

    public String getSource() {
        return source;
    }

    /**
     * 한국 표준시(Asia/Seoul, KST) 기준 LocalDateTime으로 변환하여 반환합니다.
     */
    public LocalDateTime getPublishedAt() {
        if (publishedAt == null) {
            return null;
        }
        return publishedAt.atZoneSameInstant(KST_ZONE).toLocalDateTime();
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
