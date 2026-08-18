package com.ditto.ocr.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * CLOVA OCR 호출용 RestClient 설정.
 *
 * <p>Invoke URL 은 도메인별로 전체 경로가 달라 baseUrl 로 고정하지 않고
 * 클라이언트가 호출 시점에 전체 URL 을 넘긴다.
 */
@Configuration
@EnableConfigurationProperties(OcrProperties.class)
public class OcrConfig {

    @Bean
    public RestClient clovaOcrRestClient(OcrProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.getClova().getConnectTimeout())
                .withReadTimeout(properties.getClova().getReadTimeout());

        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}
