package com.ditto.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/** 기본 추천 코스의 자리 하나. 관리자가 고치는 것은 {@code recommendationReason} 뿐이다. */
@Getter
@Builder
public class AdminSystemCoursePlaceResponse {

    private Long placeId;
    private String name;
    private String imageUrl;
    private String floorCode;
    private int visitOrder;
    private String recommendationReason;
}
