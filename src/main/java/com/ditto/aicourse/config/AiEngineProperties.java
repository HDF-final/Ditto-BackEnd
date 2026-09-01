package com.ditto.aicourse.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * AI 추천 엔진(외부 서비스) 접속 설정.
 *
 * <p>로컬은 파이썬 서비스를 HTTP 로, 배포 환경은 Lambda 를 함수 이름으로 부른다.
 * 바뀌는 것은 {@code mode} 한 줄이고 서비스 계층 코드는 그대로다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ditto.ai-engine")
public class AiEngineProperties {

    /** 엔진을 어떻게 부를 것인가 */
    private Mode mode = Mode.HTTP;

    /** HTTP 모드 전용. AI 엔진 base URL. 예: {@code http://127.0.0.1:8000} */
    private String baseUrl;

    /** HTTP 모드 전용. 대화 요청 경로 */
    private String chatPath = "/chat";

    /** LAMBDA 모드 전용. 호출할 함수 이름 또는 ARN. 예: {@code ditto-chat-v3} */
    private String functionName;

    /**
     * LAMBDA 모드 전용. 셀럽 조사 방식 — {@code tavily}(웹 검색) 또는 {@code builtin}(모델 지식).
     *
     * <p>비워 두면 요청에 안 싣고 엔진 기본값({@code tavily})을 따른다.
     * Tavily 가 느리거나 막힐 때 재배포 없이 {@code builtin} 으로 되돌리는 손잡이다.
     */
    private String engine;

    /** LAMBDA 모드 전용. 함수가 있는 리전 */
    private String region = "ap-northeast-2";

    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * 응답 대기 시간. 넉넉해야 한다 —
     * 한 턴에 LLM 을 여러 번 호출하고 웹 검색까지 붙어 수십 초가 걸린다.
     */
    private Duration readTimeout = Duration.ofSeconds(120);

    public enum Mode {

        /** HTTP 로 부른다. 인증 없는 로컬 파이썬 엔진용. */
        HTTP,

        /**
         * Lambda 를 함수 이름으로 직접 부른다.
         * SigV4 서명은 SDK 가 처리하고 자격증명은 기본 체인(EC2 인스턴스 역할)에서 나온다.
         * 역할에 {@code lambda:InvokeFunction} 권한이 있어야 한다.
         */
        LAMBDA
    }
}
