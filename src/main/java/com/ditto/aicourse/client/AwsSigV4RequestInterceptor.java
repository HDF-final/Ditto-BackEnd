package com.ditto.aicourse.client;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;

/**
 * 나가는 요청에 AWS SigV4 서명을 붙인다.
 *
 * <p>Lambda Function URL 을 {@code AuthType=AWS_IAM} 으로 열어두면 서명 없는 요청은 403 이다.
 * 서명에 쓸 자격증명은 {@link AwsCredentialsProvider} 가 공급한다 — 운영에서는
 * EC2 인스턴스 역할(IMDS)이므로 액세스 키를 파일이나 환경변수에 둘 필요가 없다.
 *
 * <p>인스턴스 역할이 주는 것은 <b>임시</b> 자격증명이라 세션 토큰이 함께 온다.
 * 서명기가 이를 {@code X-Amz-Security-Token} 헤더로 붙여주므로 여기서 따로 다루지 않는다.
 */
public class AwsSigV4RequestInterceptor implements ClientHttpRequestInterceptor {

    /** Lambda Function URL 호출 시의 서명 서비스명. */
    public static final String LAMBDA_SIGNING_NAME = "lambda";

    private final AwsV4HttpSigner signer = AwsV4HttpSigner.create();

    private final AwsCredentialsProvider credentialsProvider;
    private final String region;
    private final String signingName;

    public AwsSigV4RequestInterceptor(AwsCredentialsProvider credentialsProvider,
                                      String region, String signingName) {
        this.credentialsProvider = credentialsProvider;
        this.region = region;
        this.signingName = signingName;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        SdkHttpRequest unsigned = toSdkRequest(request);

        // 본문 해시가 서명에 들어가므로, 여기서 넘기는 body 와 실제로 나가는 body 가 같아야 한다.
        // 인터셉터 체인의 마지막에 두고 이후 아무도 본문을 건드리지 않는 이유다.
        SignedRequest signed = signer.sign(builder -> builder
                .identity(credentialsProvider.resolveCredentials())
                .request(unsigned)
                .payload(ContentStreamProvider.fromByteArray(body))
                .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, signingName)
                .putProperty(AwsV4HttpSigner.REGION_NAME, region));

        HttpHeaders headers = request.getHeaders();
        signed.request().forEachHeader((name, values) -> {
            // Host 는 전송 계층이 URI 에서 직접 채운다. 여기서 또 넣으면 클라이언트에 따라
            // 중복 헤더가 되거나 조용히 버려진다. 값은 서명한 것과 같으므로 맡겨두면 된다.
            if (!HttpHeaders.HOST.equalsIgnoreCase(name)) {
                headers.put(name, List.copyOf(values));
            }
        });

        return execution.execute(request, body);
    }

    /**
     * 서명 대상 요청을 만든다.
     *
     * <p>서명할 헤더는 최소로 둔다. {@code Accept} 나 {@code User-Agent} 처럼 전송 계층이
     * 나중에 붙이거나 바꿀 수 있는 헤더까지 서명하면 실제로 나간 값과 어긋나 403 이 된다.
     * 서명하지 않은 헤더가 함께 나가는 것은 SigV4 가 문제 삼지 않는다.
     */
    private SdkHttpRequest toSdkRequest(HttpRequest request) {
        SdkHttpRequest.Builder builder = SdkHttpRequest.builder()
                .method(SdkHttpMethod.fromValue(request.getMethod().name()))
                .uri(request.getURI());

        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType != null) {
            builder.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
        }
        return builder.build();
    }
}
