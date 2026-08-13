package com.ditto.aicourse.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 코스 추천 대화 요청.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseChatRequest {

    @Schema(description = "이어갈 대화 id. 비워 보내면 새 대화가 시작되고 서버가 새로 발급한다. "
            + "앞 응답의 sessionId 를 실으면 대화로 다듬기·재추천이 된다.",
            example = "Op3uskz8Gpo")
    @Size(max = 64)
    private String sessionId;

    @Schema(description = "손님이 한 말. 첫 요청이면 원하는 코스를, "
            + "이어지는 요청이면 고치고 싶은 점이나 \"다시 짜줘\" 를 보낸다.",
            example = "카리나가 좋아하는 브랜드 구경하고 밥도 먹고 싶어")
    @NotBlank
    @Size(max = 500)
    private String message;
}
