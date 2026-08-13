package com.ditto.navigation.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import lombok.Getter;
import lombok.Setter;

/**
 * place 테이블의 길찾기 식별자 조회 매퍼.
 */
@Mapper
public interface PlaceNavigationMapper {

    List<PlaceNavigationRow> findAllNavigable();

    PlaceNavigationRow findByPlaceId(@Param("placeId") Long placeId);

    @Getter
    @Setter
    class PlaceNavigationRow {
        private Long placeId;
        private String navigationKey;
        private String name;
        private String floorCode;
    }
}
