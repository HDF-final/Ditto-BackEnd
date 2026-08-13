package com.ditto.navigation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 길찾기 원장과 DB 장소를 연결하는 최소 장소 정보.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceNavigationResponse {

    private Long placeId;
    private String navigationKey;
    private String name;
    private String floorCode;
}
