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

    /** 인식한 브랜드명을 상호에 포함하는 장소 후보. 이름순으로 최대 {@code limit} 개. */
    List<CandidateRow> findCandidatesByBrandName(@Param("brandName") String brandName,
                                                 @Param("limit") int limit);

    @Getter
    @Setter
    class CandidateRow {
        private Long placeId;
        private String name;
        private String floor;
    }
}
