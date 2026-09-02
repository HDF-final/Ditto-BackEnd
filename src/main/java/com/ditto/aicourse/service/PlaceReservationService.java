package com.ditto.aicourse.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ditto.aicourse.dto.response.PlaceReservationResponse;
import com.ditto.aicourse.repository.PlaceReservationMapper;
import com.ditto.aicourse.repository.PlaceReservationMapper.PlaceReservationRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceReservationService {

    private static final String PROVIDER_CATCH_TABLE = "CATCH_TABLE";

    private final PlaceReservationMapper placeReservationMapper;

    /**
     * navigationKey 로 연결된 장소의 캐치테이블 예약 정보를 반환한다.
     * 예약 링크가 없는 장소면 {@code null} 을 돌려주고, 프론트는 이 경우 예약 섹션을 노출하지 않는다.
     */
    public PlaceReservationResponse getReservation(String navigationKey) {
        if (!StringUtils.hasText(navigationKey)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PlaceReservationRow row =
                placeReservationMapper.findReservationByNavigationKey(navigationKey.trim());
        if (row == null || !StringUtils.hasText(row.getCatchTableUrl())) {
            return null;
        }
        return PlaceReservationResponse.builder()
                .provider(PROVIDER_CATCH_TABLE)
                .placeName(row.getPlaceName())
                .reservationUrl(row.getCatchTableUrl())
                .build();
    }
}
