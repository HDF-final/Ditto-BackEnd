package com.ditto.user.dto.response;

import com.ditto.user.domain.Persona;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "쇼핑 페르소나 응답 DTO")
public class PersonaResponse {

    @Schema(description = "페르소나 코드", example = "OPEN_RUN_LOVER")
    private String persona;

    @Schema(description = "페르소나 한글명", example = "오픈런러버")
    private String displayName;

    @Schema(description = "페르소나 영문명", example = "OPEN-RUN LOVER")
    private String englishName;

    @Schema(description = "페르소나 설명", example = "신상·팝업 뜨면 제일 먼저")
    private String description;

    public static PersonaResponse from(String personaCode) {
        Persona persona = Persona.from(personaCode);
        if (persona == null) {
            return PersonaResponse.builder()
                    .persona(personaCode)
                    .build();
        }
        return PersonaResponse.builder()
                .persona(persona.name())
                .displayName(persona.getDisplayName())
                .englishName(persona.getEnglishName())
                .description(persona.getDescription())
                .build();
    }
}
