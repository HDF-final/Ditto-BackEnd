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
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("Python 크롤러 서비스 미응답(Connection Refused), Java 직접 크롤링(Fallback)으로 전환합니다: url={}", url);
            return crawlDirectHttp(candidate);
        } catch (RestClientException e) {
            log.warn("Python 크롤러 서비스 호출 실패: url={}, cause={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);
        }
    }

    private CrawledNewsArticle crawlDirectHttp(NewsArticleCandidate candidate) {
        String url = candidate.getUrl().trim();
        try {
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();

            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> httpResponse = httpClient.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
            String html = httpResponse.body();
            if (html == null || html.isBlank()) {
                throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);
            }

            String imageUrl = extractMetaContent(html, "property=\"og:image\"");
            if (imageUrl == null || imageUrl.isBlank()) {
                imageUrl = extractMetaContent(html, "name=\"twitter:image\"");
            }

            String description = extractMetaContent(html, "property=\"og:description\"");
            if (description == null || description.isBlank()) {
                description = extractMetaContent(html, "name=\"description\"");
            }

            String body = extractArticleBody(html);
            if (body == null || body.isBlank() || body.length() < 50) {
                body = description != null && !description.isBlank() ? description : candidate.getTitle();
            }

            return CrawledNewsArticle.builder()
                    .title(candidate.getTitle())
                    .body(body)
                    .url(url)
                    .source(candidate.getSource())
                    .publishedAt(candidate.getPublishedAt())
                    .imageUrl(imageUrl)
                    .build();

        } catch (Exception ex) {
            log.error("Java 직접 크롤링 실패: url={}, cause={}", url, ex.getMessage());
            throw new BusinessException(ErrorCode.NEWS_CRAWLING_FAILED);
        }
    }

    private String extractMetaContent(String html, String attributePattern) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<meta\\s+[^>]*" + attributePattern + "[^>]*content=[\"']([^\"']+)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return org.springframework.web.util.HtmlUtils.htmlUnescape(matcher.group(1).trim());
        }
        java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("<meta\\s+[^>]*content=[\"']([^\"']+)[\"'][^>]*" + attributePattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher2 = pattern2.matcher(html);
        if (matcher2.find()) {
            return org.springframework.web.util.HtmlUtils.htmlUnescape(matcher2.group(1).trim());
        }
        return null;
    }

    private String extractArticleBody(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        // 1. article 태그 또는 주요 본문 컨테이너 영역 추출
        String contentHtml = html;
        java.util.regex.Pattern articlePattern = java.util.regex.Pattern.compile("(?is)<(article|div[^>]*class=[\"'][^\"']*(story-news|article-text|article-body|news-article|content)[^\"']*[\"'])[^>]*>(.*?)</\\1>");
        java.util.regex.Matcher articleMatcher = articlePattern.matcher(html);
        if (articleMatcher.find()) {
            contentHtml = articleMatcher.group(3);
        }

        String cleaned = contentHtml.replaceAll("(?is)<(script|style|nav|header|footer|aside|figure|figcaption)[^>]*>.*?</\\1>", "");
        java.util.regex.Pattern pPattern = java.util.regex.Pattern.compile("(?is)<p[^>]*>(.*?)</p>");
        java.util.regex.Matcher pMatcher = pPattern.matcher(cleaned);
        StringBuilder sb = new StringBuilder();
        int pCount = 0;

        while (pMatcher.find() && pCount < 4) {
            String pText = pMatcher.group(1).replaceAll("<[^>]+>", "").trim();
            pText = org.springframework.web.util.HtmlUtils.htmlUnescape(pText);

            // 노이즈 및 부가 정보 필터링
            if (pText.startsWith("(") && pText.endsWith(")") && pText.contains("끝")) break;
            if (pText.contains("저작권자") || pText.contains("무단전재") || pText.contains("기자 =")
                    || pText.contains("촬영 ") || pText.contains("제공.") || pText.contains("배포 금지")
                    || pText.contains("yna.co.kr") || pText.contains("이전 콘텐츠") || pText.contains("다음 콘텐츠")) {
                continue;
            }

            if (pText.length() >= 25) {
                sb.append(pText).append("\n\n");
                pCount++;
            }
        }
        return sb.toString().trim();
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
