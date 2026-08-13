package com.ditto.course.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CoursePlaceResponse {

    private final Long placeId;
    private final String name;
    private final String description;
    private final String floor;
    private final String category;
    private final String imageUrl;
    private final String businessHours;
    private final String placeType;
    private final int visitOrder;
}
