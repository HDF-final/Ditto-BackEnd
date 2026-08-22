package com.ditto.aicourse.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 엔진으로 보내는 요청 본문.
 *
 * <pre>
 * {"session": "abc"|null, "message": "...", "language": "ko", "engine": "tavily"|"builtin"|null}
 * </pre>
 *
 * <p>비어 있는 필드는 싣지 않는다 — 안 실어야 엔진이 자기 기본값을 쓴다.
 * {@code null} 뿐 아니라 빈 문자열도 빼야 한다: 설정을 안 채운 채
 * {@code "engine": ""} 을 보내면 엔진이 그것을 "tavily 가 아님"으로 읽어
 * 조용히 builtin 으로 돌아간다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AiEngineChatRequest {

    /**
     * 이어갈 대화 id.
     *
     * <p>엔진이 이 값을 볼지는 엔진 쪽 사정이다 — {@code ditto-chat-v2} 는 지금
     * 턴마다 새 세션을 만들어서 이 값을 무시한다. 그래도 계속 실어 보낸다.
     * 엔진이 세션 보관을 붙이면 백엔드를 고치지 않고 멀티턴이 살아난다.
     */
    @JsonProperty("session")
    private String session;

    @JsonProperty("message")
    private String message;

    @JsonProperty("language")
    private String language;

    /**
     * 셀럽 조사 방식. {@code tavily}(웹 검색) 또는 {@code builtin}(모델 지식).
     *
     * <p>비워 두면 필드를 안 싣고 엔진 기본값을 따른다. Tavily 쪽이 문제를 일으킬 때
     * 재배포 없이 {@code builtin} 으로 되돌리는 손잡이다.
     */
    @JsonProperty("engine")
    private String engine;
}
