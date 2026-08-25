package com.ditto.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ditto.admin.client.CelebDraftClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.services.lambda.LambdaClient;

/**
 * 코스 초안 람다 클라이언트 구성.
 *
 * <p>{@link LambdaClient} 를 빈으로 올리지 않고 클라이언트 안에 감춘다.
 * {@code AiEngineConfig} 도 자기 것을 같은 방식으로 들고 있어, 두 곳이 타임아웃과
 * 재시도 정책을 각자 정할 수 있다 — 저쪽은 한 턴에 수십 초를 기다리고 이쪽은 10초에
 * 끊는다. 빈 하나를 나눠 쓰면 둘 중 하나가 남의 정책을 쓰게 된다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CelebDraftProperties.class)
public class CelebDraftConfig {

    @Bean
    public CelebDraftClient celebDraftClient(CelebDraftProperties properties, ObjectMapper objectMapper) {
        log.info("코스 초안을 Lambda 직접 호출로 읽는다. functionName={}, region={}",
                properties.getFunctionName(), properties.getRegion());
        return new CelebDraftClient(lambdaClient(properties), properties, objectMapper);
    }

    private LambdaClient lambdaClient(CelebDraftProperties properties) {
        return LambdaClient.builder()
                .region(Region.of(properties.getRegion()))
                // 자격증명은 기본 체인이 찾는다. EC2 에서는 인스턴스 역할(IMDS)이라
                // 액세스 키를 파일이나 환경변수에 둘 필요가 없다.
                .httpClientBuilder(ApacheHttpClient.builder()
                        .connectionTimeout(properties.getConnectTimeout())
                        .socketTimeout(properties.getReadTimeout()))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        // 재시도를 끈다. 조회 창구가 10초 안에 답을 못 하면 람다나 Redis 쪽
                        // 문제이지 일시적인 흔들림이 아니다 — 다시 걸어 봐야 관리자만 더 기다린다.
                        .retryStrategy(DefaultRetryStrategy.doNotRetry())
                        .apiCallAttemptTimeout(properties.getReadTimeout())
                        .apiCallTimeout(properties.getReadTimeout().plusSeconds(5))
                        .build())
                .build();
    }
}
