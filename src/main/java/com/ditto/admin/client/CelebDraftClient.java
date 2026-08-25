package com.ditto.admin.client;

import com.ditto.admin.config.CelebDraftProperties;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/**
 * {@code ditto-celeb-warm-2} 의 <b>조회 창구</b>만 부른다.
 *
 * <p>같은 함수가 명단을 받으면 초안을 <i>만들지만</i>({@code {"artists":[…]}}), 이 클라이언트는
 * 그 모양을 만들 방법이 아예 없다. 보내는 것은 아래 셋뿐이다.
 *
 * <pre>
 *   {"drafts": true}       살아 있는 초안 목록 (머리말만)
 *   {"draft": "카리나"}     초안 하나 (research·state 까지 통째로)
 *   {"run": true}          오늘 실행이 어디까지 갔나
 * </pre>
 *
 * <p>인물 이름은 언제나 <b>값</b>으로만 들어간다. 칸 이름으로 쓰이는 자리가 없으므로
 * 관리자가 어떤 이름을 넣어도 생성 경로로 새지 않는다.
 *
 * <p>Function URL 을 HTTP 로 부르지 않으므로 SigV4 서명 코드가 필요 없다 — 서명은 SDK 가
 * 하고 자격증명은 기본 체인(로컬 profile / EC2 인스턴스 역할)에서 나온다.
 * ({@code com.ditto.aicourse.client.LambdaAiEngineClient} 와 같은 판단이다.)
 */
@Slf4j
public class CelebDraftClient implements AutoCloseable {

    private final LambdaClient lambdaClient;
    private final CelebDraftProperties properties;
    private final ObjectMapper objectMapper;

    public CelebDraftClient(LambdaClient lambdaClient, CelebDraftProperties properties,
                            ObjectMapper objectMapper) {
        this.lambdaClient = lambdaClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String getFunctionName() {
        return properties.getFunctionName();
    }

    /** 살아 있는 초안 목록. {@code {"count":n,"drafts":[…]}} */
    public JsonNode listDrafts() {
        return call(payload().put("drafts", true));
    }

    /** 인물 한 명의 초안. 없으면 {@code {"celebrity":…,"error":"초안이 없습니다 …"}} */
    public JsonNode findDraft(String celebrity) {
        return call(payload().put("draft", celebrity));
    }

    /** 오늘 실행 상황. {@code {"date":…,"queued":n,"done":{이름:사유}}} */
    public JsonNode findRunStatus() {
        return call(payload().put("run", true));
    }

    @Override
    public void close() {
        lambdaClient.close();
    }

    private ObjectNode payload() {
        return objectMapper.createObjectNode();
    }

    /**
     * 창구 하나를 부르고 응답 JSON 을 그대로 돌려준다.
     *
     * <p><b>{@code {"error": …}} 는 여기서 안 막는다.</b> 그 뜻이 창구마다 다르기 때문이다 —
     * {@code {"draft":…}} 의 오류는 "그런 초안이 없다"(404)이고 {@code {"run":…}} 의 오류는
     * "Redis 에 못 붙었다"(502)다. 어느 창구를 불렀는지 아는 쪽은 서비스이므로 판단도
     * 거기서 한다({@code AdminCourseService}).
     *
     * <p>대신 <b>호출 자체가 실패한 것</b>은 전부 여기서 막는다.
     */
    private JsonNode call(ObjectNode payload) {
        try {
            InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(properties.getFunctionName())
                    .invocationType(InvocationType.REQUEST_RESPONSE)
                    .payload(SdkBytes.fromByteArray(serialize(payload)))
                    .build());

            // 핸들러가 예외를 던지면 HTTP 는 200 이고 functionError 에만 표시가 붙는다.
            // 이걸 안 보면 오류 JSON 을 초안으로 착각해 필드가 전부 null 로 흐른다.
            if (response.functionError() != null) {
                log.error("코스 초안 함수가 실패했다. functionName={}, functionError={}, errorType={}",
                        properties.getFunctionName(), response.functionError(), errorTypeOf(response));
                throw new BusinessException(ErrorCode.CELEB_DRAFT_READ_FAILED);
            }

            return read(response);

        } catch (SdkException e) {
            // 호출 자체가 실패한 경우 — 권한 부족, 자격증명 조회 실패(IMDS 미도달), 타임아웃.
            log.error("코스 초안 호출 실패. functionName={}, region={}, cause={}",
                    properties.getFunctionName(), properties.getRegion(), e.getMessage());
            throw new BusinessException(ErrorCode.CELEB_DRAFT_READ_FAILED);
        }
    }

    private JsonNode read(InvokeResponse response) {
        SdkBytes payload = response.payload();
        if (payload == null || payload.asByteArray().length == 0) {
            log.error("코스 초안 응답 본문이 비어 있다. functionName={}", properties.getFunctionName());
            throw new BusinessException(ErrorCode.CELEB_DRAFT_READ_FAILED);
        }
        try {
            return objectMapper.readTree(payload.asByteArray());
        } catch (java.io.IOException e) {
            log.error("코스 초안 응답 파싱 실패. functionName={}, cause={}",
                    properties.getFunctionName(), e.getMessage());
            throw new BusinessException(ErrorCode.CELEB_DRAFT_READ_FAILED);
        }
    }

    /**
     * 오류 payload 에서 예외 종류만 꺼낸다.
     *
     * <p>{@code errorMessage} 에는 인물 이름이 딸려 오는 일이 있어 로그에 남기지 않는다.
     * 전문이 필요하면 CloudWatch 로그를 본다.
     */
    private String errorTypeOf(InvokeResponse response) {
        SdkBytes payload = response.payload();
        if (payload == null) {
            return "unknown";
        }
        try {
            return objectMapper.readTree(payload.asByteArray()).path("errorType").asText("unknown");
        } catch (java.io.IOException e) {
            return "unknown";
        }
    }

    private byte[] serialize(ObjectNode payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            log.error("코스 초안 요청 직렬화 실패. cause={}", e.getMessage());
            throw new BusinessException(ErrorCode.CELEB_DRAFT_READ_FAILED);
        }
    }
}
