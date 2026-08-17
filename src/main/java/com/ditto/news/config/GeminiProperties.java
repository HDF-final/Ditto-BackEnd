package com.ditto.news.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Google Gemini LLM API 연동 설정 프로퍼티.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    /** Gemini API Key (환경변수 GEMINI_API_KEY) */
    private String apiKey;

    /** Gemini 엔드포인트 Base URL */
    private String baseUrl = "https://generativelanguage.googleapis.com";

    /** 사용할 Gemini 모델 식별자 (예: gemini-2.5-flash) */
    private String model = "gemini-2.5-flash";

    /** HTTP 연결 타임아웃 (초) */
    private int connectTimeoutSeconds = 5;

    /** HTTP 응답 수신 타임아웃 (초) */
    private int readTimeoutSeconds = 30;

    /** Gemini 기능 활성화 여부 */
    private boolean enabled = true;
}
