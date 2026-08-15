package com.ditto.news.pipeline;

import java.util.List;

import com.ditto.news.pipeline.model.NewsArticleCandidate;

/**
 * 사전 정의된 키워드 기반으로 기사 후보 목록을 수집하는 역할 인터페이스.
 */
public interface NewsArticleCollector {

    /**
     * 지정된 단일 키워드를 바탕으로 외부 검색 또는 RSS/API를 통해 기사 후보 목록을 수집합니다.
     *
     * @param keyword 검색 대상 키워드
     * @return 수집된 기사 후보 목록
     */
    List<NewsArticleCandidate> collect(String keyword);

    /**
     * 여러 키워드를 바탕으로 기사 후보 목록을 일괄 수집합니다.
     *
     * @param keywords 검색 대상 키워드 목록
     * @return 수집된 기사 후보 목록
     */
    List<NewsArticleCandidate> collectAll(List<String> keywords);
}
