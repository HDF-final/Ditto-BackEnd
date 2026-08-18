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
@Schema(description = "로그인 요청 DTO")
public class LoginRequest {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @JsonAlias({"email", "userEmail"})
    @Schema(description = "사용자 이메일", example = "yuki@example.com")
    private String userEmail;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Schema(description = "비밀번호", example = "password123!")
    private String password;
}
