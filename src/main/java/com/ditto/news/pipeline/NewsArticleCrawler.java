package com.ditto.news.pipeline;

import java.util.List;

import com.ditto.news.pipeline.model.CrawledNewsArticle;
import com.ditto.news.pipeline.model.NewsArticleCandidate;

/**
 * 수집된 기사 후보의 URL에 접근하여 본문 및 이미지 정보를 크롤링하는 역할 인터페이스.
 */
public interface NewsArticleCrawler {

    /**
     * 단일 기사 후보의 URL에서 기사 본문과 대표 이미지를 크롤링합니다.
     *
     * @param candidate 크롤링 대상 기사 후보
     * @return 본문과 이미지가 포함된 크롤링 결과 기사
     */
    CrawledNewsArticle crawl(NewsArticleCandidate candidate);

    /**
     * 여러 기사 후보의 본문과 이미지를 일괄 크롤링합니다.
     *
     * @param candidates 크롤링 대상 기사 후보 목록
     * @return 크롤링 결과 기사 목록
     */
    List<CrawledNewsArticle> crawlAll(List<NewsArticleCandidate> candidates);
}
