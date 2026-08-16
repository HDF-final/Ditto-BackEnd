package com.ditto.news.outbound.generator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ditto.news.application.port.out.AiNewsFeedGenerator;
import com.ditto.news.domain.CrawledNewsArticle;
import com.ditto.news.domain.GeneratedNewsFeed;
import com.ditto.news.outbound.generator.dto.GeminiNewsFeedPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google Gemini 기반 AI 뉴스피드 생성기 아웃바운드 어댑터.
 * 선별된 기사들의 사실(Fact)을 참조하여 제목, 3줄 요약, 재작성 본문, 태그를 포함한 완제품 피드를 생성하며,
 * API 미설정 또는 오류 발생 시 템플릿 기반 안전 폴백을 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiNewsFeedGenerator implements AiNewsFeedGenerator {

    private final GeminiNewsApiClient geminiClient;

    @Override
    public GeneratedNewsFeed generate(List<CrawledNewsArticle> articles, String topicKeyword) {
        if (articles == null || articles.isEmpty()) {
            log.warn("뉴스피드를 생성할 기사 목록이 비어있습니다. topic={}", topicKeyword);
            return null;
        }

        String topic = topicKeyword != null && !topicKeyword.isBlank() ? topicKeyword.trim() : "K-POP";
        String representativeImageUrl = extractRepresentativeImageUrl(articles);
        String slug = generateSlug(topic);
        String sourceAttribution = buildSourceAttribution(articles);

        // 1. Gemini LLM 구조화 생성 시도
        GeminiNewsFeedPayload payload = geminiClient.generateRewrittenNews(articles, topic);
        if (payload != null && payload.getTitle() != null && !payload.getTitle().isBlank()) {
            String title = payload.getTitle().trim();
            List<String> summaries = payload.getSummaries() != null && !payload.getSummaries().isEmpty()
                    ? payload.getSummaries()
                    : buildFallbackSummaries(articles, topic);

            StringBuilder bodyBuilder = new StringBuilder();
            if (payload.getBody() != null && !payload.getBody().isBlank()) {
                bodyBuilder.append(payload.getBody().trim());
            }
            if (!sourceAttribution.isBlank()) {
                bodyBuilder.append("\n\n").append(sourceAttribution);
            }

            List<String> keywords = payload.getKeywords() != null && !payload.getKeywords().isEmpty()
                    ? payload.getKeywords()
                    : buildDefaultKeywords(topic);

            return GeneratedNewsFeed.builder()
                    .title(title)
                    .summaries(summaries)
                    .body(bodyBuilder.toString().trim())
                    .slug(slug)
                    .representativeImageUrl(representativeImageUrl)
                    .keywords(keywords)
                    .build();
        }

        // 2. Gemini 미사용/실패 시 템플릿 기반 안전 폴백 생성
        log.info("Gemini 미사용/호출실패로 템플릿 기반 피드 합성 생성을 수행합니다. topic={}", topic);
        return generateTemplateFallback(articles, topic, representativeImageUrl, slug, sourceAttribution);
    }

    private GeneratedNewsFeed generateTemplateFallback(
            List<CrawledNewsArticle> articles,
            String topic,
            String representativeImageUrl,
            String slug,
            String sourceAttribution) {

        CrawledNewsArticle mainArticle = articles.get(0);
        String title = String.format("[%s 최신 소식] %s", topic, mainArticle.getTitle());
        List<String> summaries = buildFallbackSummaries(articles, topic);

        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append(String.format("🔥 오늘의 %s 트렌드 및 주요 소식 요약입니다.\n\n", topic));

        for (CrawledNewsArticle article : articles) {
            bodyBuilder.append(String.format("📌 [%s] %s\n",
                    article.getSource() != null ? article.getSource() : "뉴스",
                    article.getTitle()));

            if (article.getBody() != null && !article.getBody().isBlank()) {
                String clean = article.getBody().replaceAll("\\s+", " ").trim();
                String snippet = clean.length() > 200 ? clean.substring(0, 200) + "..." : clean;
                bodyBuilder.append(snippet).append("\n\n");
            }
        }

        if (!sourceAttribution.isBlank()) {
            bodyBuilder.append(sourceAttribution);
        }

        return GeneratedNewsFeed.builder()
                .title(title)
                .summaries(summaries)
                .body(bodyBuilder.toString().trim())
                .slug(slug)
                .representativeImageUrl(representativeImageUrl)
                .keywords(buildDefaultKeywords(topic))
                .build();
    }

    private List<String> buildFallbackSummaries(List<CrawledNewsArticle> articles, String topic) {
        List<String> summaries = new ArrayList<>();
        for (int i = 0; i < Math.min(articles.size(), 3); i++) {
            CrawledNewsArticle a = articles.get(i);
            summaries.add(a.getTitle());
        }
        if (summaries.isEmpty()) {
            summaries.add(topic + " 최신 트렌드 주요 소식");
        }
        return summaries;
    }

    private String extractRepresentativeImageUrl(List<CrawledNewsArticle> articles) {
        for (CrawledNewsArticle article : articles) {
            if (article.getImageUrl() != null && !article.getImageUrl().isBlank()) {
                return article.getImageUrl();
            }
        }
        return null;
    }

    private List<String> buildDefaultKeywords(String topic) {
        List<String> keywords = new ArrayList<>();
        keywords.add("#" + topic.replaceAll("\\s+", ""));
        keywords.add("#KCulture");
        keywords.add("#DITTO");
        return keywords;
    }

    private String generateSlug(String topic) {
        return topic.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String buildSourceAttribution(List<CrawledNewsArticle> articles) {
        Set<String> sources = new LinkedHashSet<>();
        for (CrawledNewsArticle a : articles) {
            if (a.getSource() != null && !a.getSource().isBlank()) {
                sources.add(a.getSource().trim());
            }
        }
        if (sources.isEmpty()) {
            return "";
        }
        return "출처: " + String.join(", ", sources);
    }
}
