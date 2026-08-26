package com.ditto.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/** 기본 추천 코스의 자리 하나. 관리자가 고치는 것은 {@code recommendationReason} 뿐이다. */
@Getter
@Builder
public class AdminSystemCoursePlaceResponse {

    private Long placeId;
    private String name;

    /**
     * 바로 쓸 수 있는 주소. 화면이 썸네일로 그린다.
     */
    private String imageUrl;

    /**
     * 같은 사진의 S3 키. <b>대표 사진으로 고를 때 이 값을 되돌려 보낸다</b> —
     * {@code COURSE.MAIN_IMAGE} 에 들어가는 것은 주소가 아니라 키다.
     */
    private String imageKey;
    private String floorCode;
    private int visitOrder;
    private String recommendationReason;
}
