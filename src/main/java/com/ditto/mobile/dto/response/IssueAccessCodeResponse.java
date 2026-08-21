package com.ditto.mobile.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 접속 코드 발급 응답.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueAccessCodeResponse {

    @Schema(description = "발급된 접속 코드", example = "7K9Q2M")
    private String accessCode;

    @Schema(description = "코드가 가리키는 코스 ID", example = "10")
    private Long courseId;

    @Schema(description = "코드 만료 시각", example = "2026-09-20T05:40:00")
    private LocalDateTime expiresAt;
}
