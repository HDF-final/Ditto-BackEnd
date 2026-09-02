package com.ditto.aicourse.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import lombok.Getter;
import lombok.Setter;

@Mapper
public interface PlaceReservationMapper {

    PlaceReservationRow findReservationByNavigationKey(
            @Param("navigationKey") String navigationKey);

    @Getter
    @Setter
    class PlaceReservationRow {
        private String placeName;
        private String catchTableUrl;
    }
}
