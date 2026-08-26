package com.ditto.ocr.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import lombok.Getter;
import lombok.Setter;

/**
 * OCR 길찾기용 place 조회 매퍼.
 */
@Mapper
public interface OcrPlaceMapper {

    /** 세션 시작 장소의 길찾기 식별자. 장소가 없거나 navigation_key 가 없으면 {@code null}. */
    String findNavigationKeyByPlaceId(@Param("placeId") Long placeId);

    /**
     * 길찾기 가능한 장소 전체. OCR 매칭은 SQL LIKE 가 아니라 애플리케이션에서
     * exact / alias / fuzzy 로 수행하므로 카탈로그를 한 번에 읽는다.
     */
    List<CandidateRow> findAllNavigablePlaces();

    @Getter
    @Setter
    class CandidateRow {
        private Long placeId;
        private String navigationKey;
        private String name;
        private String floor;
    }
}
