package com.ditto.aicourse.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.ditto.aicourse.client.AiEngineClient;
import com.ditto.aicourse.client.HttpAiEngineClient;
import com.ditto.aicourse.client.LambdaAiEngineClient;

/**
 * 설정값이 실제로 어느 구현으로 이어지는지 본다.
 *
 * <p>이게 어긋나면 조용히 틀린다 — 운영에서 mode 가 안 붙으면 HTTP 구현이 뜨고,
 * 그 구현은 로컬 주소(127.0.0.1:8000)를 부르다가 매 요청 502 를 낸다.
 * 기동 로그만 보면 정상이라 배포가 끝난 뒤에야 알게 된다.
 */
class AiEngineConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations
                    .of(JacksonAutoConfiguration.class))
            .withUserConfiguration(AiEngineConfig.class);

    @Test
    @DisplayName("운영 설정 — Lambda 를 함수 이름으로 부르는 구현이 뜬다")
    void lambdaMode() {
        runner.withPropertyValues(
                        "ditto.ai-engine.mode=lambda",
                        "ditto.ai-engine.function-name=ditto-chat-v3",
                        "ditto.ai-engine.region=ap-northeast-2")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AiEngineClient.class))
                            .isInstanceOf(LambdaAiEngineClient.class);
                    assertThat(context.getBean(AiEngineProperties.class).getFunctionName())
                            .isEqualTo("ditto-chat-v3");
                });
    }

    @Test
    @DisplayName("AI_ENGINE_ENGINE 을 안 채우면 engine 은 빈 값이고 요청에 안 실린다")
    void blankEngineStaysBlank() {
        // yml 이 ${AI_ENGINE_ENGINE:} 이라 값을 안 주면 빈 문자열이 들어온다.
        // 이게 그대로 나가면 엔진이 "tavily 아님"으로 읽어 builtin 으로 돌아간다.
        runner.withPropertyValues(
                        "ditto.ai-engine.mode=lambda",
                        "ditto.ai-engine.function-name=ditto-chat-v3",
                        "ditto.ai-engine.engine=")
                .run(context -> assertThat(
                        context.getBean(AiEngineProperties.class).getEngine()).isEmpty());
    }

    @Test
    @DisplayName("로컬 기본값 — 파이썬 엔진을 HTTP 로 부르는 구현이 뜬다")
    void httpModeIsTheDefault() {
        runner.withPropertyValues("ditto.ai-engine.base-url=http://127.0.0.1:8000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AiEngineClient.class))
                            .isInstanceOf(HttpAiEngineClient.class);
                });
    }
}
