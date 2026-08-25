package com.ditto.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ditto.admin.client.CelebApproveClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.services.lambda.LambdaClient;

/**
 * 승인 람다 클라이언트 구성. {@link CelebDraftConfig} 와 같은 짜임이되 타임아웃과
 * 재시도 정책이 다르다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CelebApproveProperties.class)
public class CelebApproveConfig {

    @Bean
    public CelebApproveClient celebApproveClient(CelebApproveProperties properties,
                                                 ObjectMapper objectMapper) {
        log.info("코스 승인을 Lambda 직접 호출로 보낸다. functionName={}, region={}",
                properties.getFunctionName(), properties.getRegion());
        return new CelebApproveClient(lambdaClient(properties), properties, objectMapper);
    }

    private LambdaClient lambdaClient(CelebApproveProperties properties) {
        return LambdaClient.builder()
                .region(Region.of(properties.getRegion()))
                .httpClientBuilder(ApacheHttpClient.builder()
                        .connectionTimeout(properties.getConnectTimeout())
                        .socketTimeout(properties.getReadTimeout()))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        // **재시도를 끈다.** 승인은 쓰기다. 타임아웃 뒤에 다시 걸면
                        // 같은 승인이 두 번 도는데, 멱등이라 결과는 같아도 Oracle
                        // MERGE 와 만료 청소를 두 번 하게 된다. 관리자가 다시 누르는
                        // 편이 낫다 — 그쪽은 무엇이 일어났는지 보고 누른다.
                        .retryStrategy(DefaultRetryStrategy.doNotRetry())
                        .apiCallAttemptTimeout(properties.getReadTimeout())
                        .apiCallTimeout(properties.getReadTimeout().plusSeconds(5))
                        .build())
                .build();
    }
}
