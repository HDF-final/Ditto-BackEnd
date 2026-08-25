package com.ditto.admin.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 트렌드 산출물 응답")
public class TrendArtifactResponse {

    @Schema(description = "산출물 코드", example = "top4")
    private String artifact;

    @Schema(description = "화면 표시명", example = "국가별 TOP 4")
    private String displayName;

    @Schema(description = "서버가 허용한 S3 object key")
    private String objectKey;

    @Schema(description = "S3 객체 최종 수정 시각")
    private Instant lastModified;

    @Schema(description = "S3 객체 ETag")
    private String eTag;

    @Schema(description = "수집 결과 상태", example = "complete")
    private String status;

    @Schema(description = "산출물 생성 시각")
    private String builtAt;

    @Schema(description = "경고 개수", example = "0")
    private int warningCount;

    @Schema(description = "Lambda가 생성한 원본 트렌드 JSON")
    private JsonNode payload;
}
