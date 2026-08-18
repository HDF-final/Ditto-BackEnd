package com.ditto.user.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "내 정보(프로필) 수정 요청 DTO")
public class UpdateUserProfileRequest {

    @Size(max = 100, message = "닉네임(이름)은 최대 100자까지 가능합니다.")
    @JsonAlias({"name", "nickname", "userName"})
    @Schema(description = "변경할 닉네임 또는 이름 (선택)", example = "새로운닉네임")
    private String nickname;

    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하이어야 합니다.")
    @Schema(description = "변경할 비밀번호 (선택)", example = "newPassword123!")
    private String password;

    @JsonAlias({"persona", "shoppingType", "personaType"})
    @Schema(
            description = "변경할 쇼핑 페르소나 (OPEN_RUN_LOVER, FLEX_SPENDER, LITTLE_JOY, ULTIMATE_STAN) (선택)",
            example = "OPEN_RUN_LOVER",
            allowableValues = {"OPEN_RUN_LOVER", "FLEX_SPENDER", "LITTLE_JOY", "ULTIMATE_STAN"}
    )
    private String persona;
}
