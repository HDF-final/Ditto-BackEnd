package com.ditto.global.infrastructure.translation;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.translate.TranslateClient;

@Configuration
@EnableConfigurationProperties(TranslationProperties.class)
public class TranslationConfig {

    @Bean(destroyMethod = "close")
    public TranslateClient translateClient(TranslationProperties properties) {
        return TranslateClient.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }

    @Bean
    @Qualifier("translationClock")
    public Clock translationClock() {
        return Clock.systemDefaultZone();
    }
}
