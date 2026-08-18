package com.ditto.aicourse.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.ditto.aicourse.client.AiEngineClient;
import com.ditto.aicourse.client.HttpAiEngineClient;
import com.ditto.aicourse.client.LambdaAiEngineClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Slf4j
@Configuration
@EnableConfigurationProperties(AiEngineProperties.class)
public class AiEngineConfig {

    /**
     * 구현을 하나만 만든다. 쓰지 않는 쪽의 커넥션 풀이나 자격증명 조회가
     * 백그라운드에 남지 않도록 필요한 것만 조립한다.
     */
    @Bean
    public AiEngineClient aiEngineClient(AiEngineProperties properties, ObjectMapper objectMapper) {
        return switch (properties.getMode()) {
            case HTTP -> {
                log.info("AI 엔진을 HTTP 로 호출한다. baseUrl={}, path={}",
                        properties.getBaseUrl(), properties.getChatPath());
                yield new HttpAiEngineClient(restClient(properties), properties, objectMapper);
            }
            case LAMBDA -> {
                log.info("AI 엔진을 Lambda 직접 호출한다. functionName={}, region={}",
                        properties.getFunctionName(), properties.getRegion());
                yield new LambdaAiEngineClient(lambdaClient(properties), properties, objectMapper);
            }
        };
    }

    private RestClient restClient(AiEngineProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.getConnectTimeout())
                .withReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

    private LambdaClient lambdaClient(AiEngineProperties properties) {
        return LambdaClient.builder()
                .region(Region.of(properties.getRegion()))
                // 자격증명은 기본 체인이 찾는다. EC2 에서는 인스턴스 역할(IMDS)이라
                // 액세스 키를 파일이나 환경변수에 둘 필요가 없다.
                .httpClientBuilder(ApacheHttpClient.builder()
                        .connectionTimeout(properties.getConnectTimeout())
                        .socketTimeout(properties.getReadTimeout()))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        // 재시도를 끈다. 한 턴에 LLM 을 여러 번 부르므로 타임아웃마다
                        // 같은 질문이 다시 실행되면 요금만 배로 나가고 응답은 더 늦어진다.
                        .retryStrategy(DefaultRetryStrategy.doNotRetry())
                        .apiCallAttemptTimeout(properties.getReadTimeout())
                        .apiCallTimeout(properties.getReadTimeout().plusSeconds(5))
                        .build())
                .build();
    }
}
