package com.ditto.aicourse.client;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.ditto.aicourse.config.AiEngineProperties;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.global.i18n.ContentLanguage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * AI 엔진을 HTTP 로 부른다. 인증 없는 로컬 파이썬 서비스를 겨냥한 구현이다.
 *
 * <p>배포 환경은 {@link LambdaAiEngineClient} 를 쓴다 — 함수 이름으로 직접 부르면
 * 서명을 SDK 가 처리해 주고, 엔진을 인터넷에 노출할 이유도 없어진다.
 */
@Slf4j
public class HttpAiEngineClient implements AiEngineClient {

    private final RestClient restClient;
    private final AiEngineProperties properties;
    private final ObjectMapper objectMapper;

    public HttpAiEngineClient(RestClient restClient, AiEngineProperties properties,
                              ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiEngineChatResponse chat(String session, String message, ContentLanguage language) {
        AiEngineChatRequest request = AiEngineChatRequest.builder()
                .session(session)
                .message(message)
                .language(language == null ? ContentLanguage.KOREAN.getCode() : language.getCode())
                .build();

        // 본문을 byte[] 로 넘겨 Content-Length 를 반드시 채운다.
        // 객체를 그대로 넘기면 JDK HttpClient 가 길이를 모른 채 chunked 로 보내는데,
        // 엔진 쪽 stdlib http.server 는 Content-Length 가 없으면 본문을 0바이트로 읽는다
        // (chat/server.py 의 `int(headers.get("Content-Length") or 0)`).
        // 그러면 에러 없이 빈 메시지가 전달된다 — 조용히 틀리는 쪽이라 반드시 막아야 한다.
        byte[] payload = serialize(request);

        try {
            AiEngineChatResponse response = restClient.post()
                    .uri(properties.getChatPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(AiEngineChatResponse.class);

            if (response == null) {
                log.error("AI 엔진 응답 본문이 비어 있다. path={}", properties.getChatPath());
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
            }
            return response;

        } catch (RestClientResponseException e) {
            log.error("AI 엔진이 오류를 반환했다. baseUrl={}, path={}, status={}",
                    properties.getBaseUrl(), properties.getChatPath(), e.getStatusCode());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);

        } catch (RestClientException e) {
            // 연결 거부 / 타임아웃을 한 곳에서 502 로 바꾼다.
            // 대화 내용은 개인정보일 수 있어 남기지 않는다.
            log.error("AI 엔진 호출 실패. baseUrl={}, path={}, cause={}",
                    properties.getBaseUrl(), properties.getChatPath(), e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private byte[] serialize(AiEngineChatRequest request) {
        try {
            return objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException e) {
            log.error("AI 엔진 요청 직렬화 실패. cause={}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }
}
