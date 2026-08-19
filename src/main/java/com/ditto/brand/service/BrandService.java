package com.ditto.brand.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.brand.dto.response.BrandResponse;
import com.ditto.brand.repository.BrandMapper;
import com.ditto.brand.repository.BrandMapper.BrandRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.global.infrastructure.s3.S3Provider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

    private final BrandMapper brandMapper;
    private final S3Provider s3Provider;

    public List<BrandResponse> getBrands() {
        return brandMapper.findAllActive().stream()
                .map(this::toResponse)
                .toList();
    }

    public BrandResponse getBrand(Long brandId) {
        if (brandId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        BrandRow row = brandMapper.findById(brandId);
        if (row == null) {
            throw new BusinessException(ErrorCode.BRAND_NOT_FOUND);
        }
        return toResponse(row);
    }

    private BrandResponse toResponse(BrandRow row) {
        return BrandResponse.builder()
                .brandId(row.getBrandId())
                .name(row.getName())
                .logoUrl(s3Provider.resolveImageUrl(row.getLogoUrl()))
                .build();
    }
}
