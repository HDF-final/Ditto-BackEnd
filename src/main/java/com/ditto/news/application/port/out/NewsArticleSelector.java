package com.ditto.news.application.port.out;

import java.util.List;

import com.ditto.news.domain.CrawledNewsArticle;

/**
 * 크롤링된 기사들 중 K-컬처 관련성과 중요도를 평가하여 피드 생성 대상 기사를 선별하는 역할 포트.
 */
public interface NewsArticleSelector {

    /**
     * 크롤링된 기사 목록에서 주어진 토픽들과 가장 연관성이 높은 기사들을 선별합니다.
     *
     * @param articles 크롤링된 기사 목록
     * @param topics 대상 K-컬처 토픽 목록
     * @return 선별된 핵심 기사 목록 (중복 제거 및 점수순 정렬)
     */
    List<CrawledNewsArticle> selectRelevantArticles(List<CrawledNewsArticle> articles, List<String> topics);
}
