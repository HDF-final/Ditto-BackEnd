package com.ditto.mobile.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 모바일 접속 코드 발급 요청.
 */
@Getter
@Setter
@NoArgsConstructor
public class IssueAccessCodeRequest {

    @Schema(description = "접속 코드를 발급할 코스 ID", example = "10")
    @NotNull(message = "코스 ID는 필수입니다.")
    private Long courseId;
}
