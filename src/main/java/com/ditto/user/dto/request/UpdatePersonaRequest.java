package com.ditto.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "쇼핑 페르소나 설정/수정 요청 DTO")
public class UpdatePersonaRequest {

    @NotBlank(message = "쇼핑 타입(페르소나)은 필수 입력값입니다.")
    @Schema(
            description = "쇼핑 타입 (OPEN_RUN_LOVER, FLEX_SPENDER, LITTLE_JOY, ULTIMATE_STAN)",
            example = "OPEN_RUN_LOVER",
            allowableValues = {"OPEN_RUN_LOVER", "FLEX_SPENDER", "LITTLE_JOY", "ULTIMATE_STAN"}
    )
    private String persona;
}
