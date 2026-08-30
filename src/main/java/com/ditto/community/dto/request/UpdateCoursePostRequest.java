package com.ditto.community.dto.request;

import java.util.List;

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
public class UpdateCoursePostRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String content;

    private List<Long> deleteImageIds;

    private Boolean deleteAllImages;
}
