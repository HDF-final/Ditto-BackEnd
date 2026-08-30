package com.ditto.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "커뮤니티 인기 장소 응답")
public class PopularPlaceResponse {

    @Schema(description = "순위", example = "1")
    private Integer rank;

    @Schema(description = "장소 ID", example = "11")
    private Long placeId;

    @Schema(description = "장소명", example = "탬버린즈")
    private String name;

    @Schema(description = "층 정보", example = "1F")
    private String floor;

    @Schema(description = "해당 장소가 포함된 커뮤니티 게시글 수", example = "3")
    private Long postCount;
}
