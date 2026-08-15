package com.ditto.news.pipeline;

import java.util.List;

import com.ditto.news.pipeline.model.CrawledNewsArticle;

/**
 * 크롤링된 기사 목록 중 K-컬처 트렌드 부합도 및 콘텐츠 품질 기준에 따라 뉴스피드 생성에 적합한 기사를 선별하는 역할 인터페이스.
 */
public interface NewsArticleSelector {

    /**
     * 크롤링된 기사 목록에서 대상 키워드와 연관성이 높고 유의미한 기사를 선별합니다.
     *
     * @param articles 크롤링된 기사 목록
     * @param targetKeywords 선별 기준 키워드 목록
     * @return 선별된 기사 목록
     */
    List<CrawledNewsArticle> selectRelevantArticles(List<CrawledNewsArticle> articles, List<String> targetKeywords);
}
