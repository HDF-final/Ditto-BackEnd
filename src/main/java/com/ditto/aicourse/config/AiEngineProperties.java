package com.ditto.aicourse.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * AI 추천 엔진(외부 서비스) 접속 설정.
 *
 * <p>엔진이 로컬 파이썬 서비스든 AWS Lambda 든 자바 코드는 그대로다.
 * 바뀌는 것은 {@code base-url}, {@code chat-path}, {@code auth} 세 줄이다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ditto.ai-engine")
public class AiEngineProperties {

    /**
     * AI 엔진 base URL.
     * 예: {@code http://127.0.0.1:8000},
     * {@code https://xxx.lambda-url.ap-northeast-2.on.aws} (끝에 / 를 붙이지 않는다)
     */
    private String baseUrl;

    /** 대화 요청 경로. Lambda Function URL 은 함수가 경로를 나누지 않으므로 보통 {@code /} 다. */
    private String chatPath = "/chat";

    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * 응답 대기 시간. 넉넉해야 한다 —
     * 첫 요청은 BGE-m3 모델 적재(약 2GB)가 붙고, 한 턴에 LLM 을 2~3회 호출한다.
     */
    private Duration readTimeout = Duration.ofSeconds(120);

    /** 엔진 호출 인증 방식 */
    private Auth auth = Auth.NONE;

    /** SigV4 서명 리전. {@code auth=aws-iam} 일 때만 쓰인다. */
    private String region = "ap-northeast-2";

    public enum Auth {

        /** 서명 없이 그대로 호출한다. 로컬 파이썬 엔진용. */
        NONE,

        /**
         * 기본 자격증명 체인으로 SigV4 서명해 호출한다.
         * Lambda Function URL 의 {@code AuthType=AWS_IAM} 용이며,
         * EC2 에서는 인스턴스 역할이 자격증명을 공급하므로 액세스 키를 어디에도 두지 않는다.
         */
        AWS_IAM
    }
}
