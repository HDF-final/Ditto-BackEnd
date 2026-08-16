package com.ditto.news.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * RSS/Atom 피드에서 1차 수집된 뉴스 기사 후보 도메인 모델.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "url")
public class NewsArticleCandidate {

    /** 기사 제목 */
    private String title;

    /** 원문 기사 URL (중복 식별 기준) */
    private String url;

    /** 언론사 / 출처 */
    private String source;

    /** 기사 발행 일시 (Asia/Seoul 기준) */
    private LocalDateTime publishedAt;

    /** 기사 요약 / 스니펫 */
    private String description;

    /** 1차 필터링에서 매칭된 K-컬처 주제/키워드 */
    private String matchedKeyword;
}
