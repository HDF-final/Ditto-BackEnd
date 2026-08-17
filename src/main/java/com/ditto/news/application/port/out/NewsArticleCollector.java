package com.ditto.news.application.port.out;

import java.util.List;

import com.ditto.news.domain.NewsArticleCandidate;

/**
 * RSS/Atom 피드 및 다양한 뉴스 소스로부터 기사 후보를 수집하는 역할 포트.
 */
public interface NewsArticleCollector {

    /**
     * 지정된 토픽 키워드에 해당하는 뉴스 기사 후보 목록을 수집합니다.
     *
     * @param keyword 수집 대상 K-컬처 토픽 키워드 (예: "K-POP", "BTS")
     * @return 수집 및 중복 제거된 기사 후보 목록
     */
    List<NewsArticleCandidate> collect(String keyword);

    /**
     * 여러 토픽 키워드에 해당하는 뉴스 기사 후보 목록을 일괄 수집합니다.
     *
     * @param keywords 수집 대상 토픽 키워드 목록
     * @return 수집 및 전체 중복 제거된 기사 후보 목록
     */
    List<NewsArticleCandidate> collectAll(List<String> keywords);
}
