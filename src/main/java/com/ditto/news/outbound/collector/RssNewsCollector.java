package com.ditto.news.outbound.collector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ditto.news.domain.NewsArticleCandidate;
import com.ditto.news.application.port.out.NewsArticleCollector;
import com.ditto.news.config.NewsFeedGenerationProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 다중 RSS/Atom 피드로부터 K-컬처 뉴스 기사 후보를 수집, 중복 제거, 1차 키워드 필터링하는 아웃바운드 어댑터.
 * {@link NewsArticleCollector} 포트를 구현합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RssNewsCollector implements NewsArticleCollector {

    private final RssFeedClient feedClient;
    private final RssXmlParser xmlParser;
    private final NewsKeywordFilter keywordFilter;
    private final NewsFeedGenerationProperties properties;

    /** 기본 지원 K-컬처 RSS 피드 목록 */
    public static final List<RssFeedSource> DEFAULT_FEEDS = List.of(
            RssFeedSource.builder()
                    .name("The Korea Herald")
                    .url("http://www.koreaherald.com/common/rss_xml.php?ct=103")
                    .defaultCategory("Culture")
                    .build(),
            RssFeedSource.builder()
                    .name("The Korea Times")
                    .url("https://www.koreatimes.co.kr/www/rss/culture.xml")
                    .defaultCategory("Culture")
                    .build(),
            RssFeedSource.builder()
                    .name("Yonhap News")
                    .url("https://www.yna.co.kr/rss/culture.xml")
                    .defaultCategory("Culture")
                    .build()
    );

    /**
     * 지정된 단일 키워드를 바탕으로 기본 RSS 피드들로부터 기사 후보를 수집하고 1차 필터링합니다.
     *
     * @param keyword 검색 대상 키워드 (예: "K-POP")
     * @return 수집된 기사 후보 목록
     */
    @Override
    public List<NewsArticleCandidate> collect(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        return collectCandidates(DEFAULT_FEEDS, List.of(keyword.trim()));
    }

    /**
     * 여러 키워드를 바탕으로 기본 RSS 피드들로부터 기사 후보 목록을 일괄 수집하고 1차 필터링합니다.
     *
     * @param keywords 검색 대상 키워드 목록
     * @return 수집된 기사 후보 목록
     */
    @Override
    public List<NewsArticleCandidate> collectAll(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }
        return collectCandidates(DEFAULT_FEEDS, keywords);
    }

    /**
     * 기본 설정된 RSS 피드들과 application.yml의 K-컬처 토픽 설정을 사용하여 기사 후보를 수집합니다.
     *
     * @return 1차 필터링 및 중복 제거가 완료된 기사 후보 목록
     */
    public List<NewsArticleCandidate> collectCandidates() {
        return collectCandidates(DEFAULT_FEEDS, properties.getTopics());
    }

    /**
     * 지정된 피드 소스들과 키워드 목록을 기반으로 기사 후보를 수집하고 1차 필터링합니다.
     *
     * @param sources  수집 대상 RSS 피드 목록
     * @param keywords 1차 필터링 키워드 목록
     * @return 필터링 및 중복 제거 완료된 후보 목록
     */
    public List<NewsArticleCandidate> collectCandidates(List<RssFeedSource> sources, List<String> keywords) {
        if (sources == null || sources.isEmpty()) {
            return Collections.emptyList();
        }

        List<NewsArticleCandidate> rawCandidates = new ArrayList<>();

        for (RssFeedSource source : sources) {
            try {
                String xml = feedClient.fetchXml(source.getUrl());
                List<NewsArticleCandidate> parsed = xmlParser.parse(xml, source.getName());
                rawCandidates.addAll(parsed);
                log.info("RSS 피드 수집 성공: feed={}, count={}", source.getName(), parsed.size());
            } catch (Exception e) {
                log.warn("RSS 피드 수집 건너뜀 (장애 발생): feed={}, url={}, cause={}",
                        source.getName(), source.getUrl(), e.getMessage());
            }
        }

        // 1. URL 중복 제거
        List<NewsArticleCandidate> deduplicated = deduplicateByUrl(rawCandidates);

        // 2. 1차 키워드 필터링
        return keywordFilter.filterByKeywords(deduplicated, keywords);
    }

    /**
     * XML 본문 목록으로부터 후보를 파싱, 중복 제거, 1차 필터링합니다 (테스트 및 오프라인 처리용).
     *
     * @param xmlList       피드 XML 문자열 목록
     * @param defaultSource 기본 출처명
     * @param keywords      1차 필터링 키워드 목록
     * @return 필터링 및 중복 제거 완료된 후보 목록
     */
    public List<NewsArticleCandidate> collectFromXmlList(List<String> xmlList, String defaultSource, List<String> keywords) {
        if (xmlList == null || xmlList.isEmpty()) {
            return Collections.emptyList();
        }

        List<NewsArticleCandidate> rawCandidates = new ArrayList<>();
        for (String xml : xmlList) {
            try {
                List<NewsArticleCandidate> parsed = xmlParser.parse(xml, defaultSource);
                rawCandidates.addAll(parsed);
            } catch (Exception e) {
                log.warn("피드 XML 파싱 오류 건너뜀: cause={}", e.getMessage());
            }
        }

        List<NewsArticleCandidate> deduplicated = deduplicateByUrl(rawCandidates);
        return keywordFilter.filterByKeywords(deduplicated, keywords);
    }

    /**
     * 단일 XML 본문으로부터 후보를 파싱, 중복 제거, 1차 필터링합니다.
     *
     * @param xmlContent    피드 XML 본문
     * @param defaultSource 기본 출처명
     * @param keywords      1차 필터링 키워드 목록
     * @return 필터링 및 중복 제거 완료된 후보 목록
     */
    public List<NewsArticleCandidate> collectFromXml(String xmlContent, String defaultSource, List<String> keywords) {
        if (xmlContent == null || xmlContent.isBlank()) {
            return Collections.emptyList();
        }
        return collectFromXmlList(List.of(xmlContent), defaultSource, keywords);
    }

    /**
     * 기사 원문 URL 기준 중복 제거 (첫 번째로 수집된 후보 유지).
     *
     * @param candidates 중복 제거 전 기사 후보 목록
     * @return URL 고유성이 보장된 기사 후보 목록
     */
    public List<NewsArticleCandidate> deduplicateByUrl(List<NewsArticleCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, NewsArticleCandidate> uniqueMap = new LinkedHashMap<>();
        for (NewsArticleCandidate candidate : candidates) {
            if (candidate != null && candidate.getUrl() != null && !candidate.getUrl().isBlank()) {
                String normalizedUrl = candidate.getUrl().trim();
                uniqueMap.putIfAbsent(normalizedUrl, candidate);
            }
        }

        return new ArrayList<>(uniqueMap.values());
    }
}
