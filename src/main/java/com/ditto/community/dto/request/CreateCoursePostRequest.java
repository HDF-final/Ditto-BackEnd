package com.ditto.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCoursePostRequest {

    @NotNull
    private Long courseId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String content;
}
