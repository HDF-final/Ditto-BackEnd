package com.ditto.aicourse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.aicourse.dto.response.PlaceReservationResponse;
import com.ditto.aicourse.repository.PlaceReservationMapper;
import com.ditto.aicourse.repository.PlaceReservationMapper.PlaceReservationRow;
import com.ditto.global.exception.BusinessException;

class PlaceReservationServiceTest {

    private PlaceReservationMapper placeReservationMapper;
    private PlaceReservationService service;

    @BeforeEach
    void setUp() {
        placeReservationMapper = mock(PlaceReservationMapper.class);
        service = new PlaceReservationService(placeReservationMapper);
    }

    @Test
    @DisplayName("navigationKey로 장소의 캐치테이블 예약 링크를 조회한다")
    void getsReservationByNavigationKey() {
        when(placeReservationMapper.findReservationByNavigationKey("B2_STORE_0012"))
                .thenReturn(row("ETF 베이커리",
                        "https://www.catchtable.net/shop/etfbakerthehyundai"));

        PlaceReservationResponse response = service.getReservation(" B2_STORE_0012 ");

        assertThat(response).isNotNull();
        assertThat(response.getProvider()).isEqualTo("CATCH_TABLE");
        assertThat(response.getPlaceName()).isEqualTo("ETF 베이커리");
        assertThat(response.getReservationUrl())
                .isEqualTo("https://www.catchtable.net/shop/etfbakerthehyundai");
        verify(placeReservationMapper).findReservationByNavigationKey("B2_STORE_0012");
    }

    @Test
    @DisplayName("예약 링크가 없는 장소는 null을 반환한다")
    void returnsNullWhenNoReservation() {
        when(placeReservationMapper.findReservationByNavigationKey("B2_STORE_9999"))
                .thenReturn(null);

        assertThat(service.getReservation("B2_STORE_9999")).isNull();
    }

    @Test
    @DisplayName("catch_table_url이 비어 있으면 null을 반환한다")
    void returnsNullWhenUrlBlank() {
        when(placeReservationMapper.findReservationByNavigationKey("B2_STORE_0012"))
                .thenReturn(row("ETF 베이커리", "  "));

        assertThat(service.getReservation("B2_STORE_0012")).isNull();
    }

    @Test
    @DisplayName("빈 navigationKey는 거절한다")
    void rejectsBlankNavigationKey() {
        assertThatThrownBy(() -> service.getReservation(" "))
                .isInstanceOf(BusinessException.class);
    }

    private PlaceReservationRow row(String placeName, String catchTableUrl) {
        PlaceReservationRow row = new PlaceReservationRow();
        row.setPlaceName(placeName);
        row.setCatchTableUrl(catchTableUrl);
        return row;
    }
}
