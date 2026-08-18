package com.ditto.user.dto.response;

import com.ditto.user.domain.Persona;
import com.ditto.user.repository.UserRow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내 정보(프로필) 응답 DTO")
public class UserProfileResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "이메일", example = "test1@naver.com")
    private String email;

    @Schema(description = "닉네임(이름)", example = "사토 유키")
    private String nickname;

    @Schema(description = "국가 ID", example = "1")
    private Long countryId;

    @Schema(description = "선호 언어 코드", example = "ko")
    private String preferredLanguageCode;

    @Schema(description = "사용자 권한", example = "ROLE_CUSTOMER")
    private String role;

    @Schema(description = "쇼핑 페르소나 코드", example = "OPEN_RUN_LOVER")
    private String persona;

    @Schema(description = "쇼핑 페르소나 한글명", example = "오픈런러버")
    private String personaDisplayName;

    public static UserProfileResponse from(UserRow user) {
        Persona persona = Persona.from(user.getPersona());
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getName())
                .countryId(user.getCountryId())
                .preferredLanguageCode(user.getPreferredLanguageCode())
                .role(user.getRole())
                .persona(user.getPersona())
                .personaDisplayName(persona != null ? persona.getDisplayName() : null)
                .build();
    }
}
