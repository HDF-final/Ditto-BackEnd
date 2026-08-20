package com.ditto.navigation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.global.infrastructure.s3.S3Provider;
import com.ditto.global.i18n.ContentLanguage;
import com.ditto.global.infrastructure.translation.ContentTranslationService;
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
    private final ContentTranslationService contentTranslationService;

    public List<PlaceNavigationResponse> getNavigablePlaces() {
        return placeNavigationMapper.findAllNavigable().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<PlaceNavigationResponse> getNavigablePlaces(ContentLanguage language) {
        return placeNavigationMapper.findAllNavigable().stream()
                .map(row -> toResponse(row, language))
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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PlaceNavigationResponse getNavigationByPlaceId(Long placeId, ContentLanguage language) {
        if (placeId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PlaceNavigationRow row = placeNavigationMapper.findByPlaceId(placeId);
        if (row == null) {
            throw new BusinessException(ErrorCode.PLACE_NOT_FOUND);
        }
        return toResponse(row, language);
    }

    private PlaceNavigationResponse toResponse(PlaceNavigationRow row) {
        return toResponse(row, ContentLanguage.KOREAN);
    }

    private PlaceNavigationResponse toResponse(PlaceNavigationRow row, ContentLanguage language) {
        boolean translate = language != null && language.requiresTranslation();
        return PlaceNavigationResponse.builder()
                .placeId(row.getPlaceId())
                .navigationKey(row.getNavigationKey())
                .name(translate
                        ? contentTranslationService.translate(
                                "place", String.valueOf(row.getPlaceId()), "name", row.getName(), language)
                        : row.getName())
                .description(translate
                        ? contentTranslationService.translate(
                                "place",
                                String.valueOf(row.getPlaceId()),
                                "description",
                                row.getDescription(),
                                language)
                        : row.getDescription())
                .floorCode(row.getFloorCode())
                .imageUrl(s3Provider.resolveImageUrl(row.getImageUrl()))
                .build();
    }
}
