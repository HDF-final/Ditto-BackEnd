package com.ditto.aicourse.client;

import com.ditto.global.i18n.ContentLanguage;

/**
 * AI 추천 엔진 호출 창구.
 *
 * <p>구현은 둘이다 — 로컬 파이썬 서비스를 부르는 {@link HttpAiEngineClient} 와
 * 배포된 Lambda 를 함수 이름으로 부르는 {@link LambdaAiEngineClient}.
 * 어느 쪽이든 서비스 계층에는 같은 응답이 올라간다.
 */
public interface AiEngineClient {

    /**
     * 엔진에 한 턴을 보낸다.
     *
     * @param session 이어갈 대화 id. {@code null} 이면 엔진이 새 대화를 만든다.
     */
    AiEngineChatResponse chat(String session, String message, ContentLanguage language);
}
