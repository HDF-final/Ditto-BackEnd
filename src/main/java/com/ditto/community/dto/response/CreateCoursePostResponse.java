package com.ditto.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CreateCoursePostResponse {

    private final Long postId;
    private final Long courseId;
    private final Long writerId;
    private final String writerNickname;
    private final String country;
    private final String title;
}
