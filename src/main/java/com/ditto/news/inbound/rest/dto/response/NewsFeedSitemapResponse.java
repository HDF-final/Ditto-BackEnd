package com.ditto.news.inbound.rest.dto.response;

import java.time.LocalDateTime;

import com.ditto.news.domain.NewsFeed;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 검색엔진 사이트맵(/sitemap.xml) 생성용 경량 뉴스피드 응답 DTO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사이트맵용 뉴스피드 경량 정보 응답")
public class NewsFeedSitemapResponse {

    @Schema(description = "URL 식별용 고유 슬러그", example = "k-pop-988bcc9e")
    private String slug;

    @Schema(description = "뉴스피드 생성 일시 (사이트맵 lastmod 용)", example = "2026-08-16T08:32:21.791")
    private LocalDateTime createdAt;

    public static NewsFeedSitemapResponse from(NewsFeed feed) {
        return NewsFeedSitemapResponse.builder()
                .slug(feed.getSlug())
                .createdAt(feed.getCreatedAt())
                .build();
    }
}
