package com.ditto.aicourse.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.ditto.aicourse.client.AwsSigV4RequestInterceptor;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

@Slf4j
@Configuration
@EnableConfigurationProperties(AiEngineProperties.class)
public class AiEngineConfig {

    @Bean
    public RestClient aiEngineRestClient(AiEngineProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.getConnectTimeout())
                .withReadTimeout(properties.getReadTimeout());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(ClientHttpRequestFactories.get(settings));

        if (properties.getAuth() == AiEngineProperties.Auth.AWS_IAM) {
            builder.requestInterceptor(new AwsSigV4RequestInterceptor(
                    // EC2 인스턴스 역할 → 컨테이너 역할 → 환경변수 → ~/.aws/credentials 순으로 찾는다.
                    // 만들 때는 네트워크를 타지 않고, 첫 서명 때 조회한 뒤 만료 전까지 캐시한다.
                    DefaultCredentialsProvider.create(),
                    properties.getRegion(),
                    AwsSigV4RequestInterceptor.LAMBDA_SIGNING_NAME));

            log.info("AI 엔진 호출에 SigV4 서명을 적용한다. region={}, baseUrl={}",
                    properties.getRegion(), properties.getBaseUrl());
        }

        return builder.build();
    }
}
