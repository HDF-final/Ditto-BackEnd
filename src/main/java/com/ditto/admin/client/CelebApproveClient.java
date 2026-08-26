package com.ditto.admin.client;

import com.ditto.admin.config.CelebApproveProperties;
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
 * {@code ditto-celeb-approve} 의 <b>쓰기 창구</b>를 부른다.
 *
 * <p>{@link CelebDraftClient} 와 갈라 둔 것은 그쪽이 "조회 창구만 부른다"를 계약으로
 * 적어 뒀기 때문이다. 한 클래스가 읽기와 쓰기를 다 하면 그 보장이 사라진다.
 *
 * <p>Function URL 을 HTTP 로 부르지 않으므로 SigV4 서명 코드가 필요 없다 — 서명은
 * SDK 가 하고 자격증명은 기본 체인(로컬 profile / EC2 인스턴스 역할)에서 나온다.
 */
@Slf4j
public class CelebApproveClient implements AutoCloseable {

    private final LambdaClient lambdaClient;
    private final CelebApproveProperties properties;
    private final ObjectMapper objectMapper;

    public CelebApproveClient(LambdaClient lambdaClient, CelebApproveProperties properties,
                              ObjectMapper objectMapper) {
        this.lambdaClient = lambdaClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String getFunctionName() {
        return properties.getFunctionName();
    }

    /**
     * 초안 하나를 승인해 손님 캐시로 올린다.
     *
     * @param draft 관리자가 고친 초안 원문. 람다가 그대로 검증한다
     * @return {@code {"ok":true,"wrote":{…},"oracle":{…}}} 또는
     *         {@code {"ok":false,"errors":[…]}}
     */
    public JsonNode approve(JsonNode draft) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("approve", draft);
        return call(payload);
    }

    @Override
    public void close() {
        lambdaClient.close();
    }

    private JsonNode call(ObjectNode payload) {
        try {
            InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(properties.getFunctionName())
                    .invocationType(InvocationType.REQUEST_RESPONSE)
                    .payload(SdkBytes.fromByteArray(serialize(payload)))
                    .build());

            // 핸들러가 예외를 던지면 HTTP 는 200 이고 functionError 에만 표시가 붙는다.
            if (response.functionError() != null) {
                log.error("승인 함수가 실패했다. functionName={}, functionError={}",
                        properties.getFunctionName(), response.functionError());
                throw new BusinessException(ErrorCode.CELEB_COURSE_APPROVE_FAILED);
            }
            return read(response);

        } catch (SdkException e) {
            // **여기가 제일 애매한 자리다.** 타임아웃이면 승인이 됐는지 안 됐는지
            // 알 수 없다. 그래서 502 로 올리고 화면이 "다시 확인하세요" 라고 한다 —
            // 승인은 멱등이라 다시 눌러도 안전하고, 목록을 새로 받으면 그 인물의
            // 초안이 사라졌는지로 결과가 보인다.
            log.error("승인 호출 실패. functionName={}, region={}, cause={}",
                    properties.getFunctionName(), properties.getRegion(), e.getMessage());
            throw new BusinessException(ErrorCode.CELEB_COURSE_APPROVE_FAILED);
        }
    }

    private JsonNode read(InvokeResponse response) {
        SdkBytes payload = response.payload();
        if (payload == null || payload.asByteArray().length == 0) {
            log.error("승인 응답 본문이 비어 있다. functionName={}", properties.getFunctionName());
            throw new BusinessException(ErrorCode.CELEB_COURSE_APPROVE_FAILED);
        }
        try {
            return objectMapper.readTree(payload.asByteArray());
        } catch (java.io.IOException e) {
            log.error("승인 응답 파싱 실패. functionName={}, cause={}",
                    properties.getFunctionName(), e.getMessage());
            throw new BusinessException(ErrorCode.CELEB_COURSE_APPROVE_FAILED);
        }
    }

    private byte[] serialize(ObjectNode payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            log.error("승인 요청 직렬화 실패. cause={}", e.getMessage());
            throw new BusinessException(ErrorCode.CELEB_COURSE_APPROVE_FAILED);
        }
    }
}
