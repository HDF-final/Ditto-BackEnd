package com.ditto.mobile.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 접속 코드 검증 요청.
 */
@Getter
@Setter
@NoArgsConstructor
public class VerifyAccessCodeRequest {

    @Schema(description = "발급받은 접속 코드", example = "7K9Q2M")
    @NotBlank(message = "접속 코드는 필수입니다.")
    private String accessCode;
}
