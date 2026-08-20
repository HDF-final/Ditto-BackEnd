package com.ditto.aicourse.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 엔진 응답 본문.
 *
 * <pre>
 * {"session": "abc", "reply": "...", "turn": 1, "llm_calls": 3,
 *  "places": [{"navigation_key": "1F_STORE_0035", "place_name": "프라다", "reason": "..."}]}
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiEngineChatResponse {

    @JsonProperty("session")
    private String session;

    @JsonProperty("reply")
    private String reply;

    @JsonProperty("turn")
    private Integer turn;

    @JsonProperty("llm_calls")
    private Integer llmCalls;

    /** 엔진이 이 턴에 쓴 시간(초). 느려질 때 읽기 타임아웃과 견주려고 받는다. */
    @JsonProperty("seconds")
    private Double seconds;

    /**
     * 앞 대화를 이어받았는가. 엔진이 세션을 Postgres 에서 찾았으면 true 다.
     * 멀티턴이 실제로 이어지는지 확인하는 값이라 로그로 남긴다.
     */
    @JsonProperty("_resumed")
    private Boolean resumed;

    @JsonProperty("_input_tokens")
    private Integer inputTokens;

    @JsonProperty("_output_tokens")
    private Integer outputTokens;

    @JsonProperty("places")
    private List<AiEnginePlace> places;
}
