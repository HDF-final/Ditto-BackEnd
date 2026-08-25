package com.ditto.admin.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.ditto.admin.config.CelebDraftProperties;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

class CelebDraftClientTest {

    private LambdaClient lambdaClient;
    private CelebDraftClient client;

    @BeforeEach
    void setUp() {
        lambdaClient = mock(LambdaClient.class);
        CelebDraftProperties properties = new CelebDraftProperties();
        properties.setFunctionName("ditto-celeb-warm-2");
        client = new CelebDraftClient(lambdaClient, properties, new ObjectMapper());
    }

    @Test
    @DisplayName("초안 목록 창구를 그대로 부른다")
    void callsDraftListWindow() {
        stubPayload("{\"count\":0,\"drafts\":[]}");

        JsonNode result = client.listDrafts();

        assertThat(sentPayload()).isEqualTo("{\"drafts\":true}");
        assertThat(result.path("count").asInt()).isZero();
    }

    @Test
    @DisplayName("초안 하나 창구는 인물 이름을 값으로만 싣는다")
    void callsDraftWindowWithNameAsValue() {
        stubPayload("{\"celebrity\":\"카리나\",\"status\":\"ok\",\"places\":[]}");

        client.findDraft("카리나");

        // 이름이 칸 이름이 아니라 값으로 들어가야 한다. 칸으로 새면 명단(celebrities·artists·
        // names·list)이 되어 조사 배치가 돌아 버린다.
        assertThat(sentPayload()).isEqualTo("{\"draft\":\"카리나\"}");
    }

    @Test
    @DisplayName("실행 상황 창구를 그대로 부른다")
    void callsRunWindow() {
        stubPayload("{\"date\":\"2026-08-25\",\"queued\":0,\"done\":{}}");

        client.findRunStatus();

        assertThat(sentPayload()).isEqualTo("{\"run\":true}");
    }

    @Test
    @DisplayName("장소 카탈로그 창구를 그대로 부른다")
    void callsPlacesWindow() {
        stubPayload("{\"count\":147,\"places\":[]}");

        client.findPlaces(false);

        assertThat(sentPayload()).isEqualTo("{\"places\":true,\"fresh\":false}");
    }

    @Test
    @DisplayName("fresh 를 주면 람다가 들고 있는 목록을 무시하게 한다")
    void callsPlacesWindowFresh() {
        stubPayload("{\"count\":147,\"places\":[]}");

        client.findPlaces(true);

        assertThat(sentPayload()).contains("\"fresh\":true");
    }

    @Test
    @DisplayName("어느 창구도 초안을 만들지 않는다 — 명단 칸을 보내는 경로가 없다")
    void neverSendsRoster() {
        stubPayload("{\"count\":0,\"drafts\":[]}");

        client.listDrafts();

        assertThat(sentPayload())
                .doesNotContain("celebrities")
                .doesNotContain("artists")
                .doesNotContain("names")
                .doesNotContain("\"list\"")
                .doesNotContain("celebrity");
    }

    @Test
    @DisplayName("{\"error\"} 는 던지지 않고 그대로 넘긴다 — 뜻이 창구마다 다르다")
    void passesErrorPayloadThrough() {
        // {"draft":…} 의 오류는 404, {"run":…} 의 오류는 502 다. 어느 창구를 불렀는지 아는
        // 쪽은 서비스이므로 판단도 거기서 한다.
        stubPayload("{\"celebrity\":\"없는사람\",\"error\":\"초안이 없습니다\"}");

        JsonNode result = client.findDraft("없는사람");

        assertThat(result.path("error").asText()).isEqualTo("초안이 없습니다");
    }

    @Test
    @DisplayName("함수가 예외를 던지면 오류 JSON 을 초안으로 삼지 않는다")
    void rejectsFunctionError() {
        // 핸들러가 raise 하면 HTTP 는 200 이고 functionError 에만 표시가 붙는다.
        when(lambdaClient.invoke(any(InvokeRequest.class))).thenReturn(
                InvokeResponse.builder()
                        .statusCode(200)
                        .functionError("Unhandled")
                        .payload(SdkBytes.fromUtf8String(
                                "{\"errorType\":\"ConnectionError\",\"errorMessage\":\"redis\"}"))
                        .build());

        assertThatThrownBy(() -> client.listDrafts())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CELEB_DRAFT_READ_FAILED);
    }

    @Test
    @DisplayName("권한 부족·자격증명 실패는 502 로 바꾼다")
    void mapsSdkFailureToBusinessException() {
        when(lambdaClient.invoke(any(InvokeRequest.class)))
                .thenThrow(SdkClientException.create("Unable to load credentials from IMDS"));

        assertThatThrownBy(() -> client.listDrafts())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CELEB_DRAFT_READ_FAILED);
    }

    @Test
    @DisplayName("본문이 비어 있으면 빈 초안으로 삼지 않는다")
    void rejectsEmptyPayload() {
        when(lambdaClient.invoke(any(InvokeRequest.class))).thenReturn(
                InvokeResponse.builder()
                        .statusCode(200)
                        .payload(SdkBytes.fromUtf8String(""))
                        .build());

        assertThatThrownBy(() -> client.listDrafts())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CELEB_DRAFT_READ_FAILED);
    }

    @Test
    @DisplayName("설정한 함수 이름으로 부른다")
    void callsConfiguredFunction() {
        stubPayload("{\"count\":0,\"drafts\":[]}");

        client.listDrafts();

        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        verify(lambdaClient).invoke(captor.capture());
        assertThat(captor.getValue().functionName()).isEqualTo("ditto-celeb-warm-2");
    }

    private String sentPayload() {
        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        verify(lambdaClient).invoke(captor.capture());
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
