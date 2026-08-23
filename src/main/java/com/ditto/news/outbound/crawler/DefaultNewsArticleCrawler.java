package com.ditto.news.outbound.crawler;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.news.application.port.out.NewsArticleCrawler;
import com.ditto.news.config.CrawlerProperties;
import com.ditto.news.domain.CrawledNewsArticle;
import com.ditto.news.domain.NewsArticleCandidate;
import com.ditto.news.outbound.crawler.dto.NewsCrawlRequest;
import com.ditto.news.outbound.crawler.dto.NewsCrawlResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link NewsArticleCrawler} 포트 구현체.
 * 수집된 기사 후보 URL에 대해 Python 크롤러 서비스(FastAPI)를 호출하여
 * 본문, 발행일, 이미지 등을 획득하고 {@link CrawledNewsArticle}로 변환합니다.
 */
@Slf4j
@Service
public class DefaultNewsArticleCrawler implements NewsArticleCrawler {

    private final RestClient restClient;
    private final CrawlerProperties properties;
    private final ObjectMapper objectMapper;

    public DefaultNewsArticleCrawler(@Qualifier("newsCrawlerRestClient") RestClient newsCrawlerRestClient,
                                     CrawlerProperties properties,
                                     ObjectMapper objectMapper) {
        this.restClient = newsCrawlerRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public CrawledNewsArticle crawl(NewsArticleCandidate candidate) {
        if (candidate == null || candidate.getUrl() == null || candidate.getUrl().isBlank()) {
            log.warn("크롤링 대상 기사 후보 또는 URL이 유효하지 않습니다.");
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String url = candidate.getUrl().trim();
        NewsCrawlRequest request = NewsCrawlRequest.builder()
                .url(url)
                .build();

        try {
            String jsonPayload = objectMapper.writeValueAsString(request);
            log.debug("Python 뉴스 크롤러 서비스 호출: url={}, path={}", url, properties.getCrawlPath());
            String responseBody = restClient.post()
                    .uri(properties.getCrawlPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonPayload)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                log.warn("크롤러 응답이 비어있습니다. url={}", url);
                throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);
            }

            NewsCrawlResponse response = objectMapper.readValue(responseBody, NewsCrawlResponse.class);
            if (response == null || response.getBody() == null || response.getBody().isBlank()) {
                log.warn("크롤러 응답 본문이 비어있습니다. url={}", url);
                throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);
            }

            return CrawledNewsArticle.builder()
                    .title(response.getTitle() != null && !response.getTitle().isBlank() ? response.getTitle() : candidate.getTitle())
                    .body(response.getBody())
                    .url(response.getUrl() != null ? response.getUrl() : url)
                    .source(response.getSource() != null ? response.getSource() : candidate.getSource())
                    .publishedAt(response.getPublishedAt() != null ? response.getPublishedAt() : candidate.getPublishedAt())
                    .imageUrl(response.getImageUrl())
                    .build();

        } catch (JsonProcessingException e) {
            log.error("크롤러 요청/응답 JSON 처리 실패: url={}, cause={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);
        } catch (RestClientException e) {
            log.warn("Python 크롤러 서비스 호출 실패: url={}, cause={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);
        }
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
}
