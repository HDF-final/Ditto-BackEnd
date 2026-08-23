package com.ditto.news.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * LLM을 통해 생성된 뉴스피드 콘텐츠 도메인 모델.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedNewsFeed {

    /** 생성된 뉴스피드 제목 */
    private String title;

    /** 기사 3줄 핵심 요약 리스트 */
    private List<String> summaries;

    /** 생성된 뉴스피드 본문 */
    private String body;

    /** URL 식별용 슬러그 */
    private String slug;

    /** 선정된 대표 이미지 URL */
    private String representativeImageUrl;

    /** 연관 키워드/태그 목록 */
    private List<String> keywords;

    /** 원문 기사 출처 URL */
    private String sourceUrl;
}
