package com.ditto.news.inbound.rest.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.ditto.news.domain.NewsFeed;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 뉴스피드 목록 조회용 요약 응답 DTO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "뉴스피드 목록 요약 응답")
public class NewsFeedSummaryResponse {

    @Schema(description = "뉴스피드 ID", example = "1")
    private Long newsFeedId;

    @Schema(description = "뉴스피드 제목", example = "K-POP 여름 시장 활활, 보이그룹 컴백 열기")
    private String title;

    @Schema(description = "URL 슬러그", example = "k-pop-c60944b5")
    private String slug;

    @Schema(description = "대표 이미지 URL", example = "https://img.yna.co.kr/photo.jpg")
    private String representativeImageUrl;

    @Schema(description = "3줄 핵심 요약 리스트")
    private List<String> summaries;

    @Schema(description = "키워드/해시태그 목록")
    private List<String> keywords;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    public static NewsFeedSummaryResponse from(NewsFeed feed) {
        if (feed == null) {
            return null;
        }
        return NewsFeedSummaryResponse.builder()
                .newsFeedId(feed.getNewsFeedId())
                .title(feed.getTitle())
                .slug(feed.getSlug())
                .representativeImageUrl(feed.getRepresentativeImageUrl())
                .summaries(feed.getSummaries())
                .keywords(feed.getKeywords())
                .createdAt(feed.getCreatedAt())
                .build();
    }
}
