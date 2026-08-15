package com.ditto.news.crawler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.news.crawler.strategy.NewsArticleParserStrategy;
import com.ditto.news.pipeline.NewsArticleCrawler;
import com.ditto.news.pipeline.model.CrawledNewsArticle;
import com.ditto.news.pipeline.model.NewsArticleCandidate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link NewsArticleCrawler} 파이프라인 인터페이스 구현체.
 * 수집된 기사 후보 URL에 대해 적합한 {@link NewsArticleParserStrategy}를 탐색하고,
 * {@link CommonNewsCrawler}를 통해 HTML Document를 획득한 후 파싱하여 {@link CrawledNewsArticle}로 변환합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultNewsArticleCrawler implements NewsArticleCrawler {

    private final CommonNewsCrawler commonNewsCrawler;
    private final List<NewsArticleParserStrategy> parserStrategies;

    @Override
    public CrawledNewsArticle crawl(NewsArticleCandidate candidate) {
        if (candidate == null || candidate.getUrl() == null || candidate.getUrl().isBlank()) {
            log.warn("크롤링 대상 기사 후보 또는 URL이 유효하지 않습니다.");
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String url = candidate.getUrl().trim();
        NewsArticleParserStrategy strategy = findStrategy(url);
        if (strategy == null) {
            log.warn("지원하지 않는 뉴스 사이트 URL입니다: url={}", url);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Document document = commonNewsCrawler.fetchDocument(url);
        return strategy.parse(candidate, document);
    }

    @Override
    public List<CrawledNewsArticle> crawlAll(List<NewsArticleCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<CrawledNewsArticle> results = new ArrayList<>();
        for (NewsArticleCandidate candidate : candidates) {
            try {
                CrawledNewsArticle article = crawl(candidate);
                if (article != null) {
                    results.add(article);
                }
            } catch (Exception e) {
                log.warn("개별 기사 크롤링 건너뜀 (오류 발생): url={}, cause={}",
                        candidate != null ? candidate.getUrl() : "null", e.getMessage());
            }
        }
        return results;
    }

    /**
     * 주어진 URL을 지원하는 Parser Strategy를 탐색합니다.
     *
     * @param url 기사 URL
     * @return 일치하는 NewsArticleParserStrategy 또는 null
     */
    public NewsArticleParserStrategy findStrategy(String url) {
        if (parserStrategies == null || parserStrategies.isEmpty()) {
            return null;
        }
        for (NewsArticleParserStrategy strategy : parserStrategies) {
            if (strategy.supports(url)) {
                return strategy;
            }
        }
        return null;
    }
}
