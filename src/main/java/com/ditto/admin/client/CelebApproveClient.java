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
 * <p>여기에 <b>읽기 창구가 하나 있다</b>({@link #listCourses()}). 승인한 결과가 지금 어떻게
 * 나가고 있는지를 아는 것은 승인 람다뿐이라 부를 곳이 여기밖에 없다 — 초안 람다는 서빙
 * 캐시를 읽는 코드가 아예 없다. 대신 {@code CelebDraftClient} 쪽 계약("조회만")은 그대로
 * 남는다.
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
        return call(payload, ErrorCode.CELEB_COURSE_APPROVE_FAILED);
    }

    /**
     * 지금 손님에게 나가고 있는 코스 목록. 머리말만 온다.
     *
     * <p>승인이 끝나면 그 인물은 초안 목록에서 사라진다 — 초안을 지우는 것이 승인의
     * 마지막 단계다. 관리자가 "올린 것이 지금 어떻게 나가나" 를 볼 자리가 이 창구다.
     *
     * @return {@code {"count":3,"courses":[…]}} 또는 {@code {"error":"Redis 에 못 붙었습니다"}}
     */
    public JsonNode listCourses() {
        return call(objectMapper.createObjectNode().put("courses", true),
                ErrorCode.CELEB_COURSE_CACHE_READ_FAILED);
    }

    /**
     * 서비스 중인 코스 하나를 <b>어드민 편집기가 아는 모양</b>으로 되돌린다.
     *
     * <p>승인이 초안을 지우므로, 올린 뒤에 고치려면 캐시에서 되짚는 수밖에 없다. 나온
     * 것을 그대로 고쳐 {@link #approve(JsonNode)} 에 다시 넣으면 덮어쓴다.
     *
     * @return 초안과 같은 칸을 가진 문서, 또는 {@code {"celebrity":…,"error":"…"}}
     */
    public JsonNode findCourse(String celebrity, String aspect) {
        // **축을 같이 보낸다.** 한 인물이 브랜드 코스와 음식 코스를 동시에 갖고 있을 수
        // 있어(warm-1 이 둘 다 만든다), 축을 안 주면 음식 카드를 열었는데 브랜드 코스가
        // 열린다 — 그대로 다시 올리면 엉뚱한 축을 덮어쓴다.
        return call(objectMapper.createObjectNode().put("course", celebrity).put("aspect", aspect),
                ErrorCode.CELEB_COURSE_CACHE_NOT_FOUND);
    }

    /**
     * 인물의 캐시를 통째로 내린다 — 코스(전 축) · 조사 재료 · 표기.
     *
     * <p><b>되돌리는 창구는 없다.</b> 다시 올리려면 배치를 돌려 초안을 새로 만들고
     * 승인한다.
     *
     * @return {@code {"revoke":["카리나"],"keys":2,"aliases":3}}
     */
    public JsonNode revoke(String celebrity) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.putArray("revoke").add(celebrity);
        return call(payload, ErrorCode.CELEB_COURSE_REVOKE_FAILED);
    }

    @Override
    public void close() {
        lambdaClient.close();
    }

    /**
     * 창구 하나를 부르고 응답 JSON 을 그대로 돌려준다.
     *
     * <p><b>오류 코드를 받는다.</b> 같은 함수를 부르지만 실패의 뜻이 창구마다 다르다 —
     * 승인이 실패한 것과 목록을 못 읽은 것을 한 코드로 올리면 화면이 "승인에 실패했습니다"
     * 라고 적는다. 관리자는 올리지도 않은 승인을 되짚게 된다.
     */
    private JsonNode call(ObjectNode payload, ErrorCode errorCode) {
        try {
            InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(properties.getFunctionName())
                    .invocationType(InvocationType.REQUEST_RESPONSE)
                    .payload(SdkBytes.fromByteArray(serialize(payload, errorCode)))
                    .build());

            // 핸들러가 예외를 던지면 HTTP 는 200 이고 functionError 에만 표시가 붙는다.
            if (response.functionError() != null) {
                log.error("승인 람다가 실패했다. functionName={}, functionError={}, code={}",
                        properties.getFunctionName(), response.functionError(), errorCode.getCode());
                throw new BusinessException(errorCode);
            }
            return read(response, errorCode);

        } catch (SdkException e) {
            // **여기가 제일 애매한 자리다.** 타임아웃이면 승인이 됐는지 안 됐는지
            // 알 수 없다. 그래서 502 로 올리고 화면이 "다시 확인하세요" 라고 한다 —
            // 승인은 멱등이라 다시 눌러도 안전하고, 목록을 새로 받으면 그 인물의
            // 초안이 사라졌는지로 결과가 보인다.
            log.error("승인 람다 호출 실패. functionName={}, region={}, code={}, cause={}",
                    properties.getFunctionName(), properties.getRegion(),
                    errorCode.getCode(), e.getMessage());
            throw new BusinessException(errorCode);
        }
    }

    private JsonNode read(InvokeResponse response, ErrorCode errorCode) {
        SdkBytes payload = response.payload();
        if (payload == null || payload.asByteArray().length == 0) {
            log.error("승인 람다 응답 본문이 비어 있다. functionName={}, code={}",
                    properties.getFunctionName(), errorCode.getCode());
            throw new BusinessException(errorCode);
        }
        try {
            return objectMapper.readTree(payload.asByteArray());
        } catch (java.io.IOException e) {
            log.error("승인 람다 응답 파싱 실패. functionName={}, code={}, cause={}",
                    properties.getFunctionName(), errorCode.getCode(), e.getMessage());
            throw new BusinessException(errorCode);
        }
    }

    private byte[] serialize(ObjectNode payload, ErrorCode errorCode) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            log.error("승인 람다 요청 직렬화 실패. code={}, cause={}", errorCode.getCode(), e.getMessage());
            throw new BusinessException(errorCode);
        }
    }
}
