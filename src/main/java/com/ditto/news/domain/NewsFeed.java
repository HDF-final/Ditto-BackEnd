package com.ditto.news.domain;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DB에 영속화된 뉴스피드 도메인 엔티티.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsFeed {

    /** 뉴스피드 PK 식별자 */
    private Long newsFeedId;

    /** 뉴스피드 제목 */
    private String title;

    /** URL 식별용 슬러그 */
    private String slug;

    /** 대표 이미지 URL */
    private String representativeImageUrl;

    /** 뉴스피드 본문 */
    private String body;

    /** 3줄 핵심 요약 리스트 */
    private List<String> summaries;

    /** 키워드/태그 목록 */
    private List<String> keywords;

    /** 원문 기사 출처 URL */
    private String sourceUrl;

    /** 피드 생성 일시 */
    private LocalDateTime createdAt;

    /** 삭제 일시 (소프트 딜리트) */
    private LocalDateTime deletedAt;
}
