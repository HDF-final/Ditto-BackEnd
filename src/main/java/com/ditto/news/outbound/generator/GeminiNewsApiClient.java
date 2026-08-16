package com.ditto.news.outbound.generator;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ditto.news.config.GeminiProperties;
import com.ditto.news.domain.CrawledNewsArticle;
import com.ditto.news.outbound.generator.dto.GeminiGenerateRequest;
import com.ditto.news.outbound.generator.dto.GeminiGenerateResponse;
import com.ditto.news.outbound.generator.dto.GeminiNewsFeedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Google Gemini REST API 호출 클라이언트.
 * 저작권 침해 방지를 위해 선별 기사들의 사실(Fact)만 참조하여 JSON 규격에 맞춘 2차 뉴스피드 데이터를 생성합니다.
 */
@Slf4j
@Component
public class GeminiNewsApiClient {

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiNewsApiClient(
            @Qualifier("geminiRestClient") RestClient geminiRestClient,
            GeminiProperties properties,
            ObjectMapper objectMapper) {
        this.restClient = geminiRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Gemini API를 호출하여 선별된 기사들을 기반으로 구조화된 JSON 뉴스피드 페이로드를 생성합니다.
     *
     * @param articles 선별된 원문 기사 목록
     * @param topic    대상 토픽 (예: "K-POP")
     * @return 파싱된 GeminiNewsFeedPayload (API 키 부재 또는 호출 실패 시 null)
     */
    public GeminiNewsFeedPayload generateRewrittenNews(List<CrawledNewsArticle> articles, String topic) {
        if (!properties.isEnabled()) {
            log.debug("Gemini 생성이 비활성화되어 있어 기본 템플릿 생성을 진행합니다.");
            return null;
        }

        if (properties.getApiKey() == null || properties.getApiKey().isBlank() || "CHANGE_ME".equalsIgnoreCase(properties.getApiKey())) {
            log.info("Gemini API Key가 설정되지 않아 기본 템플릿 생성을 진행합니다.");
            return null;
        }

        if (articles == null || articles.isEmpty()) {
            return null;
        }

        String prompt = buildPrompt(articles, topic);
        GeminiGenerateRequest request = GeminiGenerateRequest.fromPrompt(prompt);

        try {
            GeminiGenerateResponse response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", properties.getApiKey())
                            .build(properties.getModel()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiGenerateResponse.class);

            if (response != null) {
                String generatedText = response.extractGeneratedText();
                if (generatedText != null && !generatedText.isBlank()) {
                    log.info("Gemini 뉴스피드 2차 생성 원문 수신 (길이: {}자)", generatedText.length());
                    return parsePayload(generatedText);
                }
            }
        } catch (RestClientException e) {
            log.warn("Gemini REST API 호출 실패 (기본 생성 폴백): cause={}", e.getMessage());
        } catch (Exception e) {
            log.error("Gemini 뉴스피드 생성 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
        }

        return null;
    }

    private GeminiNewsFeedPayload parsePayload(String rawText) {
        try {
            String cleanedJson = cleanJsonText(rawText);
            return objectMapper.readValue(cleanedJson, GeminiNewsFeedPayload.class);
        } catch (Exception e) {
            log.warn("Gemini 응답 JSON 파싱 실패 (원문: {}): cause={}", rawText, e.getMessage());
            return null;
        }
    }

    private String cleanJsonText(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    /**
     * 저작권 보호 및 DTO 규격 매핑을 위한 JSON 강제 프롬프트를 구성합니다.
     */
    private String buildPrompt(List<CrawledNewsArticle> articles, String topic) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 DITTO의 전문 K-컬처 뉴스 에디터입니다.\n");
        sb.append("아래에 제공되는 '").append(topic).append("' 관련 기사들의 핵심 사실(Fact)만 종합하여, ");
        sb.append("저작권 보호를 위해 원문 문장을 그대로 베끼지 말고 완전히 새로운 문장 구조와 단어로 재작성(Paraphrasing)하세요.\n\n");

        sb.append("[출력 규격: 반드시 아래 JSON 형식으로만 응답하세요. 다른 설명이나 마크다운 백틱 없이 순수 JSON만 반환하세요.]\n");
        sb.append("{\n");
        sb.append("  \"title\": \"대표 헤드라인 1줄\",\n");
        sb.append("  \"summaries\": [\n");
        sb.append("    \"첫 번째 핵심 요약 문장\",\n");
        sb.append("    \"두 번째 핵심 요약 문장\",\n");
        sb.append("    \"세 번째 핵심 요약 문장\"\n");
        sb.append("  ],\n");
        sb.append("  \"body\": \"상세 재작성 본문 (2~3개 문단으로 자연스러운 어조 서술)\",\n");
        sb.append("  \"keywords\": [\"#주요키워드1\", \"#주요키워드2\", \"#주요키워드3\"]\n");
        sb.append("}\n\n");

        sb.append("[참고 기사 팩트 정보]\n");
        int idx = 1;
        for (CrawledNewsArticle article : articles) {
            sb.append(String.format("기사 %d.\n- 제목: %s\n- 출처: %s\n- 내용: %s\n\n",
                    idx++,
                    article.getTitle(),
                    article.getSource() != null ? article.getSource() : "언론사",
                    truncateBody(article.getBody(), 500)));
        }

        return sb.toString();
    }

    private String truncateBody(String body, int maxLength) {
        if (body == null) {
            return "";
        }
        String cleaned = body.replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) + "..." : cleaned;
    }
}
