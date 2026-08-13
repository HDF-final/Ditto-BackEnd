package com.ditto.navigation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.navigation.dto.response.PlaceNavigationResponse;
import com.ditto.navigation.repository.PlaceNavigationMapper;
import com.ditto.navigation.repository.PlaceNavigationMapper.PlaceNavigationRow;

@ExtendWith(MockitoExtension.class)
class PlaceNavigationServiceTest {

    @Mock
    private PlaceNavigationMapper placeNavigationMapper;

    @InjectMocks
    private PlaceNavigationService placeNavigationService;

    @Test
    void getNavigablePlacesReturnsMappedPlaces() {
        PlaceNavigationRow row = row(11L, "B2_STORE_0032", "MLB", "B2");
        given(placeNavigationMapper.findAllNavigable()).willReturn(List.of(row));

        List<PlaceNavigationResponse> result = placeNavigationService.getNavigablePlaces();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPlaceId()).isEqualTo(11L);
        assertThat(result.get(0).getNavigationKey()).isEqualTo("B2_STORE_0032");
        assertThat(result.get(0).getName()).isEqualTo("MLB");
        assertThat(result.get(0).getFloorCode()).isEqualTo("B2");
    }

    @Test
    void getNavigationByPlaceIdRejectsNullId() {
        assertThatThrownBy(() -> placeNavigationService.getNavigationByPlaceId(null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    void getNavigationByPlaceIdThrowsWhenPlaceIsNotNavigable() {
        given(placeNavigationMapper.findByPlaceId(999L)).willReturn(null);

        assertThatThrownBy(() -> placeNavigationService.getNavigationByPlaceId(999L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PLACE_NOT_FOUND));
    }

    private PlaceNavigationRow row(
            Long placeId,
            String navigationKey,
            String name,
            String floorCode) {
        PlaceNavigationRow row = new PlaceNavigationRow();
        row.setPlaceId(placeId);
        row.setNavigationKey(navigationKey);
        row.setName(name);
        row.setFloorCode(floorCode);
        return row;
    }
}
