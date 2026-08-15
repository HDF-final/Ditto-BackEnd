package com.ditto.news.pipeline.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 기사 후보 수집 단계에서 전달되는 기사 메타데이터 모델.
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
