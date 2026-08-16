package com.ditto.news.outbound.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.ditto.news.config.GeminiProperties;
import com.ditto.news.domain.CrawledNewsArticle;
import com.ditto.news.outbound.generator.dto.GeminiNewsFeedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;

class GeminiNewsApiClientTest {

    private GeminiProperties properties;
    private MockRestServiceServer mockServer;
    private GeminiNewsApiClient apiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new GeminiProperties();
        properties.setApiKey("test-api-key");
        properties.setBaseUrl("https://generativelanguage.googleapis.com");
        properties.setModel("gemini-2.5-flash");
        properties.setEnabled(true);

        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        apiClient = new GeminiNewsApiClient(restClient, properties, objectMapper);
    }

    @Test
    @DisplayName("Gemini API 호출 성공 시 응답 JSON을 DTO(GeminiNewsFeedPayload)로 정확히 파싱한다")
    void callsGeminiApiAndExtractsPayload() {
        CrawledNewsArticle article = CrawledNewsArticle.builder()
                .title("BTS 월드투어 일정 공개")
                .body("방탄소년단이 내년 대규모 스타디움 월드투어에 돌입합니다.")
                .url("https://www.yna.co.kr/view/1")
                .source("Yonhap News")
                .build();

        String rawModelJson = """
                {
                  "title": "BTS 스타디움 월드투어 돌풍 예고",
                  "summaries": [
                    "방탄소년단 내년 대규모 스타디움 월드투어 발표",
                    "서울을 시작으로 북미와 유럽 주요 도시 순회",
                    "글로벌 팬덤의 폭발적인 티켓 예매 열기 기대"
                  ],
                  "body": "글로벌 팝 아이콘 방탄소년단이 대규모 투어 일정을 발표하며 팬들의 기대를 모으고 있습니다.",
                  "keywords": ["#BTS", "#월드투어", "#KPOP"]
                }
                """;

        String escapedText = rawModelJson.replace("\"", "\\\"").replace("\n", "\\n");

        String responseJson = String.format("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "%s"
                          }
                        ],
                        "role": "model"
                      },
                      "finishReason": "STOP"
                    }
                  ]
                }
                """, escapedText);

        mockServer.expect(method(HttpMethod.POST))
                .andExpect(queryParam("key", "test-api-key"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        GeminiNewsFeedPayload result = apiClient.generateRewrittenNews(List.of(article), "K-POP");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("BTS 스타디움 월드투어 돌풍 예고");
        assertThat(result.getSummaries()).hasSize(3);
        assertThat(result.getSummaries().get(0)).contains("방탄소년단 내년 대규모 스타디움 월드투어 발표");
        assertThat(result.getBody()).contains("글로벌 팝 아이콘 방탄소년단이 대규모 투어 일정을 발표");
        assertThat(result.getKeywords()).containsExactly("#BTS", "#월드투어", "#KPOP");
        mockServer.verify();
    }

    @Test
    @DisplayName("Gemini API 호출 실패 시 예외를 던지지 않고 안전하게 null을 반환하여 폴백을 유도한다")
    void returnsNullOnApiError() {
        CrawledNewsArticle article = CrawledNewsArticle.builder()
                .title("New Jeans Comeback")
                .body("Content")
                .url("https://www.yna.co.kr/view/1")
                .build();

        mockServer.expect(method(HttpMethod.POST))
                .andRespond(withServerError());

        GeminiNewsFeedPayload result = apiClient.generateRewrittenNews(List.of(article), "K-POP");

        assertThat(result).isNull();
        mockServer.verify();
    }

    @Test
    @DisplayName("API Key가 비어있거나 비활성화된 경우 API를 호출하지 않고 즉시 null을 반환한다")
    void returnsNullWhenApiKeyMissingOrDisabled() {
        properties.setApiKey("");
        assertThat(apiClient.generateRewrittenNews(List.of(CrawledNewsArticle.builder().title("T").build()), "K-POP")).isNull();

        properties.setApiKey("test-key");
        properties.setEnabled(false);
        assertThat(apiClient.generateRewrittenNews(List.of(CrawledNewsArticle.builder().title("T").build()), "K-POP")).isNull();

        properties.setEnabled(true);
        assertThat(apiClient.generateRewrittenNews(Collections.emptyList(), "K-POP")).isNull();
    }
}
