package com.ditto.course.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import lombok.Getter;
import lombok.Setter;

/**
 * place 테이블 조회 매퍼. SQL 은 resources/mapper/PlaceMapper.xml 에 있다.
 */
@Mapper
public interface PlaceMapper {

    List<Long> findExistingIds(@Param("placeIds") List<Long> placeIds);

    List<PlaceRow> findByIds(@Param("placeIds") List<Long> placeIds);

    @Getter
    @Setter
    class PlaceRow {
        private Long placeId;
        private String name;
        private String description;
        private String floor;
        private String category;
        private String imageUrl;
        private String businessHours;
        private String placeType;
    }
}
