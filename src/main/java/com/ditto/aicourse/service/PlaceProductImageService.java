package com.ditto.aicourse.service;

import java.net.URI;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ditto.aicourse.dto.response.PlaceProductImageResponse;
import com.ditto.aicourse.repository.PlaceProductMapper;
import com.ditto.aicourse.repository.PlaceProductMapper.PlaceProductImageRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.global.infrastructure.s3.S3Provider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceProductImageService {

    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_LIMIT = 20;

    private final PlaceProductMapper placeProductMapper;
    private final S3Provider s3Provider;

    public List<PlaceProductImageResponse> getProductImages(String navigationKey, Integer limit) {
        if (!StringUtils.hasText(navigationKey)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        int normalizedLimit = normalizeLimit(limit);
        return placeProductMapper
                .findProductImagesByNavigationKey(navigationKey.trim(), normalizedLimit)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return limit;
    }

    private PlaceProductImageResponse toResponse(PlaceProductImageRow row) {
        return PlaceProductImageResponse.builder()
                .productId(row.getProductId())
                .productName(row.getProductName())
                .brandId(row.getBrandId())
                .brandName(row.getBrandName())
                .imageUrl(resolveImageUrl(row.getImageUrl()))
                .productUrl(row.getProductUrl())
                .build();
    }

    private String resolveImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        String trimmed = imageUrl.trim();
        if (isAbsoluteHttpUrl(trimmed)) {
            return trimmed;
        }
        return s3Provider.resolveImageUrl(trimmed);
    }

    private boolean isAbsoluteHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && StringUtils.hasText(uri.getHost());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
