package com.ditto.news.inbound.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 뉴스피드 파이프라인 수동 디버깅 요청 DTO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "뉴스피드 파이프라인 디버깅 요청")
public class NewsPipelineDebugRequest {

    @NotBlank(message = "토픽은 필수입니다.")
    @Schema(description = "수집 및 생성 대상 K-컬처 토픽", example = "K-POP")
    @Builder.Default
    private String topic = "K-POP";
}
