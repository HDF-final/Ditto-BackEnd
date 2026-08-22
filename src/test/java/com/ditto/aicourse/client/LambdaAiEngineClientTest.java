package com.ditto.aicourse.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.ditto.aicourse.config.AiEngineProperties;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.global.i18n.ContentLanguage;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

class LambdaAiEngineClientTest {

    private LambdaClient lambdaClient;
    private LambdaAiEngineClient client;

    @BeforeEach
    void setUp() {
        lambdaClient = mock(LambdaClient.class);
        AiEngineProperties properties = new AiEngineProperties();
        properties.setMode(AiEngineProperties.Mode.LAMBDA);
        properties.setFunctionName("ditto-chat-v2");
        client = new LambdaAiEngineClient(lambdaClient, properties, new ObjectMapper());
    }

    @Test
    @DisplayName("엔진이 돌려준 코스를 그대로 읽어 온다")
    void readsEngineResponse() {
        stubPayload("""
                {"session":"abc","reply":"준비했습니다","turn":1,"llm_calls":3,"seconds":14.5,
                 "places":[{"navigation_key":"4F_STORE_0058","place_name":"굿러너컴퍼니","reason":"러닝"}]}
                """);

        AiEngineChatResponse response = client.chat(
                null, "러닝화 보고 싶어", ContentLanguage.KOREAN);

        assertThat(response.getSession()).isEqualTo("abc");
        assertThat(response.getTurn()).isEqualTo(1);
        assertThat(response.getPlaces()).hasSize(1);
        assertThat(response.getPlaces().get(0).getNavigationKey()).isEqualTo("4F_STORE_0058");
    }

    @Test
    @DisplayName("핸들러가 받는 모양 그대로 보낸다 — 함수 이름과 session·message")
    void sendsHandlerContract() {
        stubPayload("{\"session\":\"abc\",\"reply\":\"ok\",\"turn\":2,\"places\":[]}");

        client.chat("abc", "세 군데만 알려줘", ContentLanguage.ENGLISH);

        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        org.mockito.Mockito.verify(lambdaClient).invoke(captor.capture());
        InvokeRequest sent = captor.getValue();

        assertThat(sent.functionName()).isEqualTo("ditto-chat-v2");
        // Function URL 이 아니라 직접 호출이므로 이벤트가 곧 본문이다.
        // requestContext 를 싣지 않아야 핸들러가 invoke 경로로 처리한다.
        assertThat(sent.payload().asUtf8String())
                .contains("\"session\":\"abc\"")
                .contains("\"message\":\"세 군데만 알려줘\"")
                .contains("\"language\":\"en\"")
                .doesNotContain("requestContext")
                .doesNotContain("body");
    }

    @Test
    @DisplayName("함수가 예외를 던지면 오류 JSON 을 정상 응답으로 삼지 않는다")
    void rejectsFunctionError() {
        // 핸들러는 4xx·5xx 상황에서 RuntimeError 를 raise 한다. 그러면 HTTP 는 200 이고
        // functionError 에만 표시가 붙어, 이를 놓치면 필드가 전부 null 로 흘러간다.
        when(lambdaClient.invoke(any(InvokeRequest.class))).thenReturn(
                InvokeResponse.builder()
                        .statusCode(200)
                        .functionError("Unhandled")
                        .payload(SdkBytes.fromUtf8String(
                                "{\"errorType\":\"RuntimeError\",\"errorMessage\":\"message 가 비어 있습니다\"}"))
                        .build());

        assertThatThrownBy(() -> client.chat(null, "안녕", ContentLanguage.KOREAN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_SERVICE_ERROR);
    }

    @Test
    @DisplayName("권한 부족·자격증명 실패는 502 로 바꾼다")
    void mapsSdkFailureToBusinessException() {
        when(lambdaClient.invoke(any(InvokeRequest.class)))
                .thenThrow(SdkClientException.create("Unable to load credentials from IMDS"));

        assertThatThrownBy(() -> client.chat(null, "안녕", ContentLanguage.KOREAN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_SERVICE_ERROR);
    }

    @Test
    @DisplayName("엔진이 {\"error\"} 를 200 으로 돌려줘도 정상 응답으로 삼지 않는다")
    void rejectsErrorPayload() {
        // ditto-chat-v2 는 실패를 예외로 올리지 않는다. HTTP 200, functionError 없음,
        // 본문만 {"error": ...} 다. 이걸 놓치면 필드가 전부 null 인 코스가
        // success:true 로 손님에게 나간다.
        stubPayload("{\"error\":\"message 가 비어 있습니다\"}");

        assertThatThrownBy(() -> client.chat(null, "안녕", ContentLanguage.KOREAN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_SERVICE_ERROR);
    }

    @Test
    @DisplayName("engine 을 안 정하면 필드를 아예 싣지 않는다 — 엔진 기본값을 쓰게")
    void omitsEngineWhenUnset() {
        stubPayload("{\"session\":\"s\",\"reply\":\"r\",\"turn\":1,\"places\":[]}");

        client.chat(null, "코스 짜줘", ContentLanguage.KOREAN);

        assertThat(sentPayload()).doesNotContain("engine");
    }

    @Test
    @DisplayName("engine 을 정하면 그대로 실어 보낸다")
    void sendsEngineWhenSet() {
        AiEngineProperties properties = new AiEngineProperties();
        properties.setFunctionName("ditto-chat-v2");
        properties.setEngine("builtin");
        client = new LambdaAiEngineClient(lambdaClient, properties, new ObjectMapper());
        stubPayload("{\"session\":\"s\",\"reply\":\"r\",\"turn\":1,\"places\":[]}");

        client.chat(null, "코스 짜줘", ContentLanguage.KOREAN);

        assertThat(sentPayload()).contains("\"engine\":\"builtin\"");
    }

    private String sentPayload() {
        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        org.mockito.Mockito.verify(lambdaClient).invoke(captor.capture());
        return captor.getValue().payload().asUtf8String();
    }

    private void stubPayload(String json) {
        when(lambdaClient.invoke(any(InvokeRequest.class))).thenReturn(
                InvokeResponse.builder()
                        .statusCode(200)
                        .payload(SdkBytes.fromByteArray(json.getBytes(StandardCharsets.UTF_8)))
                        .build());
    }
}
