package com.ditto.brand.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브랜드 기본 정보. logoUrl 은 S3 object key 를 presigned URL 로 변환한 값이다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponse {

    private Long brandId;
    private String name;
    private String logoUrl;
}
