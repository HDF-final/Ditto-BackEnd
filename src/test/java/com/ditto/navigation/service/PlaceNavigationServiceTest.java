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
import com.ditto.global.infrastructure.s3.S3Provider;
import com.ditto.global.infrastructure.translation.ContentTranslationService;
import com.ditto.global.i18n.ContentLanguage;
import com.ditto.navigation.dto.response.PlaceNavigationResponse;
import com.ditto.navigation.repository.PlaceNavigationMapper;
import com.ditto.navigation.repository.PlaceNavigationMapper.PlaceNavigationRow;

@ExtendWith(MockitoExtension.class)
class PlaceNavigationServiceTest {

    @Mock
    private PlaceNavigationMapper placeNavigationMapper;

    @Mock
    private S3Provider s3Provider;

    @Mock
    private ContentTranslationService contentTranslationService;

    @InjectMocks
    private PlaceNavigationService placeNavigationService;

    @Test
    void getNavigablePlacesReturnsMappedPlaces() {
        PlaceNavigationRow row = row(11L, "B2_STORE_0032", "MLB", "B2", "place-picture/11_MLB.jpg");
        given(placeNavigationMapper.findAllNavigable()).willReturn(List.of(row));
        given(s3Provider.resolveImageUrl("place-picture/11_MLB.jpg"))
                .willReturn("https://cdn.example.com/place-picture/11_MLB.jpg");

        List<PlaceNavigationResponse> result = placeNavigationService.getNavigablePlaces();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPlaceId()).isEqualTo(11L);
        assertThat(result.get(0).getNavigationKey()).isEqualTo("B2_STORE_0032");
        assertThat(result.get(0).getName()).isEqualTo("MLB");
        assertThat(result.get(0).getFloorCode()).isEqualTo("B2");
        assertThat(result.get(0).getImageUrl())
                .isEqualTo("https://cdn.example.com/place-picture/11_MLB.jpg");
    }

    @Test
    void localizesPlaceNameAndDescription() {
        PlaceNavigationRow row = row(11L, "B2_STORE_0032", "매장", "B2", null);
        row.setDescription("설명");
        given(placeNavigationMapper.findByPlaceId(11L)).willReturn(row);
        given(contentTranslationService.translate(
                "place", "11", "name", "매장", ContentLanguage.JAPANESE))
                .willReturn("店舗");
        given(contentTranslationService.translate(
                "place", "11", "description", "설명", ContentLanguage.JAPANESE))
                .willReturn("説明");

        PlaceNavigationResponse result = placeNavigationService.getNavigationByPlaceId(
                11L, ContentLanguage.JAPANESE);

        assertThat(result.getName()).isEqualTo("店舗");
        assertThat(result.getDescription()).isEqualTo("説明");
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
            String floorCode,
            String imageUrl) {
        PlaceNavigationRow row = new PlaceNavigationRow();
        row.setPlaceId(placeId);
        row.setNavigationKey(navigationKey);
        row.setName(name);
        row.setFloorCode(floorCode);
        row.setImageUrl(imageUrl);
        return row;
    }
}
