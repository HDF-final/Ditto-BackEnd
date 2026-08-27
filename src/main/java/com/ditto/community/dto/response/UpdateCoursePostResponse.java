package com.ditto.community.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UpdateCoursePostResponse {

    private final Long postId;
    private final String title;
    private final String content;
    private final List<String> imageUrls;
    private final List<PostImageResponse> images;
}
