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
 * 저작권 침해 방지를 위해 선별 기사들의 사실(Fact)만 참조하여 DITTO 매거진 스타일의 2차 뉴스피드 데이터를 생성합니다.
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
     * 프론트엔드 UI 디자인에 맞춘 고품질 매거진 형식 프롬프트 구성.
     */
    private String buildPrompt(List<CrawledNewsArticle> articles, String topic) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 글로벌 K-컬처 여행/트렌드 플랫폼 'DITTO'의 전문 에디터입니다.\n");
        sb.append("아래에 제공되는 '").append(topic).append("' 관련 기사들의 핵심 사실(Fact)을 종합하여, ");
        sb.append("원문 문장을 그대로 베끼지 말고 저작권 침해가 없는 세련되고 매력적인 매거진 아티클로 재작성하세요.\n\n");

        sb.append("### 작성 가이드라인:\n");
        sb.append("1. **제목(title)**: 언론사명이나 대괄호([K-POP]) 없이, 핵심 트렌드를 한눈에 보여주는 매력적인 헤드라인 1줄 (예: 'K-뷰티 수출 사상 최대, 인디 브랜드가 성장을 이끌었다').\n");
        sb.append("2. **기사 요약(summaries)**: 오른쪽 요약 카드에 들어갈 3개의 핵심 인사이트 문장 (단순 제목 나열이 아닌, 1줄씩 간결하고 명확한 요약문).\n");
        sb.append("3. **본문(body)**:\n");
        sb.append("   - 3~4개의 문단으로 매끄럽게 작성.\n");
        sb.append("   - **DITTO Trend Lab 인용구 블록**: 문단 사이에 반드시 해당 기사의 핵심 사건과 아티스트에 맞춘 날카롭고 고유한 분석 인사이트 1문장을 인용구 형식으로 작성하세요. (절대 모든 기사에 '현장에서 체감하는 문화 경험...' 같은 뻔한 복붙 문구를 쓰지 말고, 해당 기사의 실제 주인공과 트렌드에 직결되는 고유 인사이트 작성).\n");
        sb.append("     * 예시(스트레이 키즈): “스트레이 키즈의 빌보드 9연속 1위는 단순 음원 성적을 넘어, K-POP 아티스트의 강력한 IP가 오프라인 성지순례와 글로벌 투어 소비로 확장되는 결정적 전환점입니다.” - DITTO Trend Lab\n");
        sb.append("     * 예시(보이그룹 컴백): “8월 대형 보이그룹들의 연이은 컴백 대전은 팬덤 중심의 앨범 팝업스토어 및 현장 체험 수요를 단기간에 집중시키는 기폭제가 되고 있습니다.” - DITTO Trend Lab\n");
        sb.append("   - **마무리 제언 문단**: 해당 기사의 주인공/사건과 관련된 맞춤형 DITTO 코스 연계 팁(예: 소속사/뮤비 촬영지 주변 성지순례 스팟, 관련 팝업스토어 추천 등)을 자연스럽게 1~2문장으로 결론짓기.\n");
        sb.append("4. **키워드(keywords)**: 본문과 관련된 구체적인 해시태그 3~4개. '#KPOP', '#트렌드' 같은 뻔한 일반 태그만 넣지 말고, 기사의 실제 주인공인 아티스트명/그룹명(예: '#스트레이키즈', '#빅뱅', '#엔하이픈', '#NCT127', '#뉴진스')과 핵심 사건(예: '#빌보드1위', '#컴백대전', '#신보발매', '#월드투어') 중심의 고유 태그를 우선 포함하세요. (예: [\"#스트레이키즈\", \"#빌보드200\", \"#보이그룹\"]).\n\n");

        sb.append("### 출력 규격 (반드시 순수 JSON만 반환):\n");
        sb.append("{\n");
        sb.append("  \"title\": \"매력적인 대표 헤드라인\",\n");
        sb.append("  \"summaries\": [\n");
        sb.append("    \"첫 번째 핵심 트렌드 요약\",\n");
        sb.append("    \"두 번째 주요 사실/성장 요약\",\n");
        sb.append("    \"세 번째 전망/소비 패턴 요약\"\n");
        sb.append("  ],\n");
        sb.append("  \"body\": \"첫 번째 문단\\n\\n두 번째 문단\\n\\n“핵심 인사이트 인용문”\\n- DITTO Trend Lab\\n\\n마무리 문단\",\n");
        sb.append("  \"keywords\": [\"#키워드1\", \"#키워드2\", \"#키워드3\"]\n");
        sb.append("}\n\n");

        sb.append("[참고 원문 팩트 정보]\n");
        int idx = 1;
        for (CrawledNewsArticle article : articles) {
            sb.append(String.format("기사 %d.\n- 제목: %s\n- 출처: %s\n- 내용: %s\n\n",
                    idx++,
                    cleanArticleTitle(article.getTitle()),
                    article.getSource() != null ? article.getSource() : "언론사",
                    truncateBody(article.getBody(), 600)));
        }

        return sb.toString();
    }

    private String cleanArticleTitle(String title) {
        if (title == null) return "";
        return title.replaceAll("\\[.*?\\]", "")
                    .replaceAll("\\|.*$", "")
                    .replaceAll("-.*$", "")
                    .trim();
    }

    private String truncateBody(String body, int maxLength) {
        if (body == null) {
            return "";
        }
        String cleaned = body.replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) + "..." : cleaned;
    }
}
