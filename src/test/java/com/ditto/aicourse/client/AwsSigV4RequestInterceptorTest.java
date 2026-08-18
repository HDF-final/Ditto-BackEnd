package com.ditto.aicourse.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * Function URL 이 {@code AuthType=AWS_IAM} 이면 서명이 조금이라도 어긋난 순간 403 이고,
 * 본문에는 이유가 없다. 그래서 "무엇을 서명하고 무엇을 보내는가" 를 여기서 못 박아 둔다.
 */
class AwsSigV4RequestInterceptorTest {

    private static final URI FUNCTION_URL =
            URI.create("https://zhh2dum6qpg2c4rzk66isuz4ba0cumkq.lambda-url.ap-northeast-2.on.aws/");
    private static final String REGION = "ap-northeast-2";
    private static final byte[] BODY =
            "{\"message\":\"러닝화 보고 싶어\"}".getBytes(StandardCharsets.UTF_8);

    private static final ClientHttpRequestExecution NOOP_EXECUTION =
            (request, body) -> new MockClientHttpResponse(new byte[0], HttpStatus.OK);

    @Test
    @DisplayName("lambda 서비스·설정 리전으로 SigV4 Authorization 헤더를 붙인다")
    void signsWithLambdaServiceAndConfiguredRegion() throws IOException {
        MockClientHttpRequest request = newRequest();

        interceptorWith(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("AKIAEXAMPLE", "secret")))
                .intercept(request, BODY, NOOP_EXECUTION);

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertThat(authorization)
                .startsWith("AWS4-HMAC-SHA256 Credential=AKIAEXAMPLE/")
                .contains("/" + REGION + "/lambda/aws4_request")
                .contains("Signature=");
        assertThat(request.getHeaders().getFirst("X-Amz-Date")).isNotNull();
    }

    @Test
    @DisplayName("서명한 헤더는 content-type 과 host 뿐이다")
    void signsOnlyStableHeaders() throws IOException {
        MockClientHttpRequest request = newRequest();
        // 전송 계층이 나중에 붙이거나 바꿀 수 있는 헤더. 서명에 들어가면 실제 전송값과 어긋나 403 이 된다.
        request.getHeaders().set(HttpHeaders.USER_AGENT, "ditto-test");
        request.getHeaders().setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        interceptorWith(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("AKIAEXAMPLE", "secret")))
                .intercept(request, BODY, NOOP_EXECUTION);

        String signedHeaders = signedHeadersOf(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertThat(signedHeaders.split(";")).containsExactlyInAnyOrder(
                "content-type", "host", "x-amz-content-sha256", "x-amz-date");
        // 서명기가 만든 본문 해시 헤더는 반드시 함께 나가야 한다. 빠지면 서명 검증이 깨진다.
        assertThat(request.getHeaders().getFirst("X-Amz-Content-Sha256")).isNotNull();
    }

    @Test
    @DisplayName("인스턴스 역할의 임시 자격증명이면 세션 토큰이 함께 실린다")
    void carriesSessionTokenForTemporaryCredentials() throws IOException {
        MockClientHttpRequest request = newRequest();

        interceptorWith(StaticCredentialsProvider.create(
                AwsSessionCredentials.create("ASIAEXAMPLE", "secret", "session-token")))
                .intercept(request, BODY, NOOP_EXECUTION);

        assertThat(request.getHeaders().getFirst("X-Amz-Security-Token")).isEqualTo("session-token");
    }

    @Test
    @DisplayName("Host 는 전송 계층에 맡기고 요청 헤더에 넣지 않는다")
    void leavesHostToTransportLayer() throws IOException {
        MockClientHttpRequest request = newRequest();

        interceptorWith(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("AKIAEXAMPLE", "secret")))
                .intercept(request, BODY, NOOP_EXECUTION);

        assertThat(request.getHeaders().getFirst(HttpHeaders.HOST)).isNull();
        // 그래도 서명에는 들어가 있어야 한다 — 전송 계층이 URI 에서 같은 값을 채운다.
        assertThat(signedHeadersOf(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)))
                .contains("host");
    }

    private static MockClientHttpRequest newRequest() {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, FUNCTION_URL);
        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return request;
    }

    private static AwsSigV4RequestInterceptor interceptorWith(StaticCredentialsProvider credentials) {
        return new AwsSigV4RequestInterceptor(
                credentials, REGION, AwsSigV4RequestInterceptor.LAMBDA_SIGNING_NAME);
    }

    /** {@code ... SignedHeaders=content-type;host;x-amz-date, Signature=...} 에서 가운데만 꺼낸다. */
    private static String signedHeadersOf(String authorization) {
        assertThat(authorization).contains("SignedHeaders=");
        String tail = authorization.substring(authorization.indexOf("SignedHeaders=") + "SignedHeaders=".length());
        return tail.substring(0, tail.indexOf(','));
    }
}
