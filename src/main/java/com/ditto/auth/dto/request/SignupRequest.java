package com.ditto.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "회원가입 요청 DTO")
public class SignupRequest {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @JsonAlias({"email", "userEmail"})
    @Schema(description = "사용자 이메일", example = "yuki@example.com")
    private String userEmail;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Schema(description = "비밀번호", example = "password123!")
    private String password;

    @NotBlank(message = "이름(닉네임)은 필수 입력값입니다.")
    @JsonAlias({"name", "nickname", "userName"})
    @Schema(description = "이름 또는 닉네임", example = "사토 유키")
    private String name;

    @NotBlank(message = "국가 코드는 필수 입력값입니다.")
    @JsonAlias({"countryCode", "country"})
    @Schema(description = "국가 코드 (2자리 알파벳)", example = "KR")
    private String countryCode;

    @Schema(description = "선호 언어 코드 (미입력 시 국가 기본 언어)", example = "ko")
    private String languageCode;

    @JsonAlias({"persona", "shoppingType", "personaType"})
    @Schema(description = "쇼핑 페르소나 (OPEN_RUN_LOVER, FLEX_SPENDER, LITTLE_JOY, ULTIMATE_STAN)", example = "OPEN_RUN_LOVER")
    private String persona;
}
