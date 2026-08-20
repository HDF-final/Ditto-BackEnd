package com.ditto.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "국가·언어 환경설정 변경 요청 DTO")
public class UpdateUserPreferencesRequest {

    @NotBlank(message = "국가 코드는 필수 입력값입니다.")
    @Size(min = 2, max = 2, message = "국가 코드는 2자리여야 합니다.")
    @Schema(description = "콘텐츠와 트렌드에 사용할 국가 코드", example = "JP")
    private String countryCode;

    @NotBlank(message = "언어 코드는 필수 입력값입니다.")
    @Size(min = 2, max = 2, message = "언어 코드는 2자리여야 합니다.")
    @Schema(description = "화면과 콘텐츠에 사용할 언어 코드", example = "en")
    private String languageCode;
}
