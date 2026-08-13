package com.ditto.aicourse.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * AI 추천 엔진(외부 서비스) 접속 설정.
 *
 * <p>지금은 로컬 파이썬 서비스를 가리키고, AWS Lambda 로 이전하면 {@code base-url} 만 바꾼다.
 * 자바 쪽 코드는 그대로다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ditto.ai-engine")
public class AiEngineProperties {

    /** AI 엔진 base URL. 예: http://127.0.0.1:8000 */
    private String baseUrl;

    /** 대화 요청 경로 */
    private String chatPath = "/chat";

    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * 응답 대기 시간. 넉넉해야 한다 —
     * 첫 요청은 BGE-m3 모델 적재(약 2GB)가 붙고, 한 턴에 LLM 을 2~3회 호출한다.
     */
    private Duration readTimeout = Duration.ofSeconds(120);
}
