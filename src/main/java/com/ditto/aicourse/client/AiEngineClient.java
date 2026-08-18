package com.ditto.aicourse.client;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.ditto.aicourse.config.AiEngineProperties;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;

/**
 * AI 추천 엔진 HTTP 클라이언트.
 *
 * <p>엔진이 로컬 파이썬 서비스든 AWS Lambda 든 이 클래스는 바뀌지 않는다.
 * 바뀌는 것은 {@code ditto.ai-engine.base-url} 한 줄이다.
 */
@Slf4j
@Component
public class AiEngineClient {

    private final RestClient restClient;
    private final AiEngineProperties properties;
    private final ObjectMapper objectMapper;

    public AiEngineClient(RestClient aiEngineRestClient, AiEngineProperties properties,
                          ObjectMapper objectMapper) {
        this.restClient = aiEngineRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 엔진에 한 턴을 보낸다.
     *
     * @param session 이어갈 대화 id. {@code null} 이면 엔진이 새 대화를 만든다.
     */
    public AiEngineChatResponse chat(String session, String message) {
        AiEngineChatRequest request = AiEngineChatRequest.builder()
                .session(session)
                .message(message)
                .build();

        // 본문을 byte[] 로 넘겨 Content-Length 를 반드시 채운다.
        // 객체를 그대로 넘기면 JDK HttpClient 가 길이를 모른 채 chunked 로 보내는데,
        // 엔진 쪽 stdlib http.server 는 Content-Length 가 없으면 본문을 0바이트로 읽는다
        // (chat/server.py 의 `int(headers.get("Content-Length") or 0)`).
        // 그러면 에러 없이 빈 메시지가 전달된다 — 조용히 틀리는 쪽이라 반드시 막아야 한다.
        // AWS API Gateway 도 chunked 를 받지 않으므로 Lambda 이전 후에도 이 방식이 맞다.
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
            // 엔진이 응답은 했으나 4xx·5xx 인 경우. 상태 코드를 남겨야 원인을 좁힐 수 있다 —
            // Lambda Function URL 의 403 은 대개 서명 실패나 IAM 권한 누락이고 본문에는 단서가 없다.
            log.error("AI 엔진이 오류를 반환했다. baseUrl={}, path={}, status={}",
                    properties.getBaseUrl(), properties.getChatPath(), e.getStatusCode());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);

        } catch (RestClientException e) {
            // 연결 거부 / 타임아웃 / 자격증명 조회 실패를 한 곳에서 502 로 바꾼다.
            // 대화 내용은 개인정보일 수 있어 남기지 않는다.
            log.error("AI 엔진 호출 실패. baseUrl={}, path={}, cause={}",
                    properties.getBaseUrl(), properties.getChatPath(), e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);

        } catch (SdkException e) {
            // SigV4 서명에 쓸 자격증명을 못 구한 경우 — EC2 라면 IMDS 미도달(도커 hop limit 등)이 대표적이다.
            // RestClientException 계층이 아니라서 그냥 두면 500 으로 새어나간다. 여기서 502 로 맞춘다.
            log.error("AI 엔진 호출용 AWS 자격증명 확보 실패. region={}, cause={}",
                    properties.getRegion(), e.getMessage());
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
