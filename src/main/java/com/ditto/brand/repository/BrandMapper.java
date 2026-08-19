package com.ditto.brand.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import lombok.Getter;
import lombok.Setter;

/**
 * brand 테이블 조회 매퍼.
 */
@Mapper
public interface BrandMapper {

    List<BrandRow> findAllActive();

    BrandRow findById(@Param("brandId") Long brandId);

    @Getter
    @Setter
    class BrandRow {
        private Long brandId;
        private String name;
        private String logoUrl;
    }
}
