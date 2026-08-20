package com.ditto.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 국가·언어 환경설정 응답 DTO")
public class UserPreferencesResponse {

    @Schema(description = "콘텐츠와 트렌드에 사용할 국가 코드", example = "JP")
    private String countryCode;

    @Schema(description = "화면과 콘텐츠에 사용할 언어 코드", example = "en")
    private String languageCode;
}
