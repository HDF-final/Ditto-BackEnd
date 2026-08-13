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

    @JsonProperty("places")
    private List<AiEnginePlace> places;
}
