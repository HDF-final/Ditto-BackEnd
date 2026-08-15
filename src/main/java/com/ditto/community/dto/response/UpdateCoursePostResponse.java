package com.ditto.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UpdateCoursePostResponse {

    private final Long postId;
    private final String title;
}
