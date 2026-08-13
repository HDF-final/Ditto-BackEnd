package com.ditto.aicourse.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 엔진으로 보내는 요청 본문. 엔진 쪽 계약은 {@code {"session": ..., "message": ...}} 이다.
 *
 * <p>{@code session} 이 없으면 엔진이 새 대화를 만들고 id 를 돌려준다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiEngineChatRequest {

    @JsonProperty("session")
    private String session;

    @JsonProperty("message")
    private String message;
}
