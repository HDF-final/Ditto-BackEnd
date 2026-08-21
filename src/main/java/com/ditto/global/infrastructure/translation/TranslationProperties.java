package com.ditto.global.infrastructure.translation;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.translation")
public class TranslationProperties {

    private boolean enabled;
    private String region = "ap-northeast-2";
    private int maxRequestBytes = 9_000;
    private Duration pendingLease = Duration.ofMinutes(2);
    private Duration retryBase = Duration.ofMinutes(5);
    private Duration retryMax = Duration.ofHours(24);
}
