package com.ditto.aicourse.client;

import com.ditto.aicourse.config.AiEngineProperties;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.global.i18n.ContentLanguage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/**
 * AI 엔진을 Lambda 함수 이름으로 직접 호출한다.
 *
 * <p>Function URL 을 HTTP 로 부르지 않으므로 SigV4 서명 코드가 필요 없다 —
 * 서명은 SDK 가 하고, 자격증명은 EC2 인스턴스 역할에서 나온다.
 * 필요한 권한은 {@code lambda:InvokeFunction} 이다.
 *
 * <p>엔진 핸들러는 직접 호출과 Function URL 을 모두 받도록 짜여 있고
 * ({@code handler._payload}), 직접 호출이면 응답 본문을 그대로 돌려준다.
 * 그래서 보내는 것도 받는 것도 HTTP 때와 같은 JSON 이다.
 */
@Slf4j
public class LambdaAiEngineClient implements AiEngineClient, AutoCloseable {

    private final LambdaClient lambdaClient;
    private final AiEngineProperties properties;
    private final ObjectMapper objectMapper;

    public LambdaAiEngineClient(LambdaClient lambdaClient, AiEngineProperties properties,
                                ObjectMapper objectMapper) {
        this.lambdaClient = lambdaClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiEngineChatResponse chat(String session, String message, ContentLanguage language) {
        AiEngineChatRequest request = AiEngineChatRequest.builder()
                .session(session)
                .message(message)
                .language(language == null ? ContentLanguage.KOREAN.getCode() : language.getCode())
                .engine(properties.getEngine())
                .build();

        try {
            InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(properties.getFunctionName())
                    .invocationType(InvocationType.REQUEST_RESPONSE)
                    .payload(SdkBytes.fromByteArray(serialize(request)))
                    .build());

            // 핸들러가 예외를 던지면 HTTP 는 200 이고 functionError 에 표시가 붙는다.
            // 이걸 안 보면 오류 JSON 을 정상 응답으로 착각해 필드가 전부 null 로 흐른다.
            if (response.functionError() != null) {
                log.error("AI 엔진 함수가 실패했다. functionName={}, functionError={}, errorType={}",
                        properties.getFunctionName(), response.functionError(),
                        errorTypeOf(response));
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
            }

            return deserialize(response);

        } catch (SdkException e) {
            // 호출 자체가 실패한 경우 — 권한 부족, 자격증명 조회 실패(IMDS 미도달), 타임아웃.
            // 대화 내용은 개인정보일 수 있어 남기지 않는다.
            log.error("AI 엔진 호출 실패. functionName={}, region={}, cause={}",
                    properties.getFunctionName(), properties.getRegion(), e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Override
    public void close() {
        lambdaClient.close();
    }

    private AiEngineChatResponse deserialize(InvokeResponse response) {
        SdkBytes payload = response.payload();
        if (payload == null || payload.asByteArray().length == 0) {
            log.error("AI 엔진 응답 본문이 비어 있다. functionName={}", properties.getFunctionName());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload.asByteArray());
        } catch (java.io.IOException e) {
            log.error("AI 엔진 응답 파싱 실패. functionName={}, cause={}",
                    properties.getFunctionName(), e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }

        // 엔진은 실패를 예외로 올리지 않고 {"error": "..."} 를 200 으로 돌려준다.
        // functionError 만 보면 이걸 정상 응답으로 삼아 필드가 전부 null 인 코스가
        // success:true 로 나간다 — 조용히 틀리는 쪽이라 반드시 여기서 막는다.
        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            log.error("AI 엔진이 오류를 돌려줬다. functionName={}, error={}",
                    properties.getFunctionName(), summarize(error.asText("")));
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }

        try {
            return objectMapper.treeToValue(root, AiEngineChatResponse.class);
        } catch (JsonProcessingException e) {
            log.error("AI 엔진 응답 파싱 실패. functionName={}, cause={}",
                    properties.getFunctionName(), e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    /**
     * 오류 문구에서 예외 종류만 남긴다.
     *
     * <p>{@code "KeyError: '카리나가 좋아하는…'"} 처럼 파이썬 예외 메시지에 손님이 보낸 말이
     * 딸려 오는 일이 있어 뒤쪽을 버린다. 전문이 필요하면 CloudWatch 로그를 본다.
     */
    private String summarize(String message) {
        int colon = message.indexOf(':');
        return colon > 0 ? message.substring(0, colon) : message;
    }

    /**
     * 오류 payload 에서 예외 종류만 꺼낸다.
     *
     * <p>{@code errorMessage} 는 손님이 보낸 말을 품고 있을 수 있어 로그에 남기지 않는다.
     */
    private String errorTypeOf(InvokeResponse response) {
        SdkBytes payload = response.payload();
        if (payload == null) {
            return "unknown";
        }
        try {
            JsonNode node = objectMapper.readTree(payload.asByteArray());
            return node.path("errorType").asText("unknown");
        } catch (java.io.IOException e) {
            return "unknown";
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
