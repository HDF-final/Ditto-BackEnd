package com.ditto.navigation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.global.infrastructure.s3.S3Provider;
import com.ditto.navigation.dto.response.PlaceNavigationResponse;
import com.ditto.navigation.repository.PlaceNavigationMapper;
import com.ditto.navigation.repository.PlaceNavigationMapper.PlaceNavigationRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceNavigationService {

    private final PlaceNavigationMapper placeNavigationMapper;
    private final S3Provider s3Provider;

    public List<PlaceNavigationResponse> getNavigablePlaces() {
        return placeNavigationMapper.findAllNavigable().stream()
                .map(this::toResponse)
                .toList();
    }

    public PlaceNavigationResponse getNavigationByPlaceId(Long placeId) {
        if (placeId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PlaceNavigationRow row = placeNavigationMapper.findByPlaceId(placeId);
        if (row == null) {
            throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
        }
        return toResponse(row);
    }

    private PlaceNavigationResponse toResponse(PlaceNavigationRow row) {
        return PlaceNavigationResponse.builder()
                .placeId(row.getPlaceId())
                .navigationKey(row.getNavigationKey())
                .name(row.getName())
                .floorCode(row.getFloorCode())
                .imageUrl(s3Provider.resolveImageUrl(row.getImageUrl()))
                .build();
    }
}
