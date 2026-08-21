package com.ditto.mobile.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 모바일 접속 설정 활성화.
 */
@Configuration
@EnableConfigurationProperties(MobileProperties.class)
public class MobileConfig {
}
