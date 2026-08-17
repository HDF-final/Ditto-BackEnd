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
 * 프론트엔드 UI 디자인에 맞춘 세련된 매거진 아티클 형식(헤드라인, 3줄 요약, 본문 서식, 인용구, 태그)을 생성합니다.
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
            String title = cleanHeadline(payload.getTitle());
            List<String> summaries = payload.getSummaries() != null && !payload.getSummaries().isEmpty()
                    ? payload.getSummaries()
                    : buildEditorialSummaries(articles, topic);

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

        // 2. Gemini 미사용/실패 시 매거진 템플릿 기반 안전 폴백 생성
        log.info("Gemini 미사용/호출실패로 고품질 매거진 템플릿 피드 생성을 수행합니다. topic={}", topic);
        return generateMagazineFallback(articles, topic, representativeImageUrl, slug, sourceAttribution);
    }

    private GeneratedNewsFeed generateMagazineFallback(
            List<CrawledNewsArticle> articles,
            String topic,
            String representativeImageUrl,
            String slug,
            String sourceAttribution) {

        CrawledNewsArticle mainArticle = articles.get(0);
        String title = cleanHeadline(mainArticle.getTitle());
        List<String> summaries = buildEditorialSummaries(articles, topic);

        StringBuilder bodyBuilder = new StringBuilder();

        // 1문단: 배경 및 도입
        bodyBuilder.append(String.format("글로벌 %s 트렌드가 빠르게 확산되며 팬덤 중심의 소비와 새로운 콘텐츠 경험이 국내외 시장의 흐름을 바꾸고 있습니다.\n\n", topic));

        // 2문단: 세부 사실 및 현장 이야기
        if (mainArticle.getBody() != null && !mainArticle.getBody().isBlank()) {
            String clean = mainArticle.getBody().replaceAll("\\s+", " ").trim();
            String snippet = clean.length() > 250 ? clean.substring(0, 250) + "..." : clean;
            bodyBuilder.append(snippet).append("\n\n");
        }

        // 3문단: 인용구 블록 (UI 디자인 매거진 스타일)
        bodyBuilder.append(String.format("“현장에서 체감하는 %s 문화 경험이 귀국 후 소비와 글로벌 팬덤으로 이어지는 핵심 동력입니다.”\n- DITTO Trend Lab\n\n", topic));

        // 4문단: DITTO 코스 연계 및 마무리 제언
        bodyBuilder.append(String.format("DITTO는 앞으로도 %s 콘텐츠와 실제 여행 경험이 만나는 스팟을 지속적으로 추적합니다. 사용자가 저장한 코스와 뉴스 관심사를 연결해 실제 방문 가능한 추천 스팟으로 확장할 예정입니다.", topic));

        if (!sourceAttribution.isBlank()) {
            bodyBuilder.append("\n\n").append(sourceAttribution);
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

    private List<String> buildEditorialSummaries(List<CrawledNewsArticle> articles, String topic) {
        List<String> summaries = new ArrayList<>();
        for (CrawledNewsArticle a : articles) {
            String cleaned = cleanHeadline(a.getTitle());
            if (!cleaned.isBlank()) {
                summaries.add(cleaned);
            }
            if (summaries.size() >= 3) break;
        }
        while (summaries.size() < 3) {
            if (summaries.isEmpty()) summaries.add(topic + " 글로벌 트렌드 및 시장 확장세 지속");
            else if (summaries.size() == 1) summaries.add("팬덤 및 현장 경험 중심의 새로운 소비 흐름 형성");
            else summaries.add("여행과 K-컬처가 만나는 주요 브랜드 스팟 주목");
        }
        return summaries;
    }

    private String cleanHeadline(String rawTitle) {
        if (rawTitle == null) return "";
        return rawTitle.replaceAll("\\[.*?\\]", "")
                .replaceAll("\\|.*$", "")
                .replaceAll(" - .*$", "")
                .replaceAll("(?i)yna|연합뉴스|korea herald|korea times", "")
                .replaceAll("^[\\s:·,]+", "")
                .replaceAll("[\\s:·,]+$", "")
                .trim();
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
        keywords.add("#" + topic.replaceAll("[^a-zA-Z0-9가-힣]", ""));
        keywords.add("#KCulture");
        keywords.add("#트렌드");
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
