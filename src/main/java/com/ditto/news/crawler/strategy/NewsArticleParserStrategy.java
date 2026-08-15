package com.ditto.news.crawler.strategy;

import org.jsoup.nodes.Document;

import com.ditto.news.pipeline.model.CrawledNewsArticle;
import com.ditto.news.pipeline.model.NewsArticleCandidate;

/**
 * 뉴스 사이트별 기사 상세 HTML 파싱 전략 인터페이스.
 */
public interface NewsArticleParserStrategy {

    /**
     * 해당 URL 또는 호스트를 이 Strategy가 지원하는지 검증합니다.
     * URI 호스트 기반으로 엄격하게 검증하여 위장 URL을 방어합니다.
     *
     * @param url 기사 원문 URL
     * @return 지원 대상 여부 (유효하지 않거나 미지원 사이트인 경우 false)
     */
    boolean supports(String url);

    /**
     * HTML Document로부터 기사 상세 데이터를 추출하여 {@link CrawledNewsArticle}로 변환합니다.
     *
     * @param candidate 원본 RSS 기사 후보 메타데이터
     * @param document  Jsoup으로 파싱된 기사 상세 페이지 HTML Document
     * @return 추출 및 정제된 CrawledNewsArticle
     */
    CrawledNewsArticle parse(NewsArticleCandidate candidate, Document document);
}
