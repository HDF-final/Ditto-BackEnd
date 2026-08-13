package com.ditto.course.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoursePlaceResponse {

    private Long placeId;
    private String name;
    private String floorCode;
    private int visitOrder;
    private String recommendationReason;
    private String visitStatus;
    private LocalDateTime visitedAt;
}
