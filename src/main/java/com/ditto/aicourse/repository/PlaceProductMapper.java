package com.ditto.aicourse.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import lombok.Getter;
import lombok.Setter;

@Mapper
public interface PlaceProductMapper {

    List<PlaceProductImageRow> findProductImagesByNavigationKey(
            @Param("navigationKey") String navigationKey,
            @Param("limit") int limit);

    @Getter
    @Setter
    class PlaceProductImageRow {
        private Long productId;
        private String productName;
        private Long brandId;
        private String brandName;
        private String imageUrl;
        private String productUrl;
    }
}
