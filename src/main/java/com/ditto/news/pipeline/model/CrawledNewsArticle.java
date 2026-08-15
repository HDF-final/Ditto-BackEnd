package com.ditto.news.pipeline.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기사 본문 크롤링 단계의 결과 모델.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawledNewsArticle {

    /** 기사 제목 */
    private String title;

    /** 기사 본문 내용 */
    private String body;

    /** 원문 기사 URL */
    private String url;

    /** 언론사 / 출처 */
    private String source;

    /** 기사 발행 일시 */
    private LocalDateTime publishedAt;

    /** 기사 내 대표/추출 이미지 URL */
    private String imageUrl;
}
