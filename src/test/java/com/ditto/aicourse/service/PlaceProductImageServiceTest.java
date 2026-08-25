package com.ditto.aicourse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ditto.aicourse.dto.response.PlaceProductImageResponse;
import com.ditto.aicourse.repository.PlaceProductMapper;
import com.ditto.aicourse.repository.PlaceProductMapper.PlaceProductImageRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.infrastructure.s3.S3Provider;

class PlaceProductImageServiceTest {

    private PlaceProductMapper placeProductMapper;
    private S3Provider s3Provider;
    private PlaceProductImageService service;

    @BeforeEach
    void setUp() {
        placeProductMapper = mock(PlaceProductMapper.class);
        s3Provider = mock(S3Provider.class);
        service = new PlaceProductImageService(placeProductMapper, s3Provider);
    }

    @Test
    @DisplayName("navigationKey로 매장 브랜드의 상품 이미지를 조회한다")
    void getsProductImagesByNavigationKey() {
        PlaceProductImageRow row = row(
                10L,
                "뉴발란스 574",
                3L,
                "뉴발란스",
                "https://image.example.com/nb-574.jpg",
                "https://www.nbkorea.com/product/574");
        when(placeProductMapper.findProductImagesByNavigationKey("B2_STORE_0012", 3))
                .thenReturn(List.of(row));

        List<PlaceProductImageResponse> responses =
                service.getProductImages(" B2_STORE_0012 ", 3);

        assertThat(responses).hasSize(1);
        PlaceProductImageResponse response = responses.get(0);
        assertThat(response.getProductId()).isEqualTo(10L);
        assertThat(response.getProductName()).isEqualTo("뉴발란스 574");
        assertThat(response.getBrandName()).isEqualTo("뉴발란스");
        assertThat(response.getImageUrl()).isEqualTo("https://image.example.com/nb-574.jpg");
        assertThat(response.getProductUrl()).isEqualTo("https://www.nbkorea.com/product/574");
        verify(placeProductMapper).findProductImagesByNavigationKey("B2_STORE_0012", 3);
    }

    @Test
    @DisplayName("limit이 없으면 기본 6개로 조회한다")
    void usesDefaultLimit() {
        when(placeProductMapper.findProductImagesByNavigationKey("B2_STORE_0012", 6))
                .thenReturn(List.of());

        service.getProductImages("B2_STORE_0012", null);

        verify(placeProductMapper).findProductImagesByNavigationKey("B2_STORE_0012", 6);
    }

    @Test
    @DisplayName("S3 object key 형태의 이미지는 조회 URL로 변환한다")
    void resolvesS3ObjectKeyImage() {
        PlaceProductImageRow row = row(
                11L,
                "뉴발란스 가방",
                3L,
                "뉴발란스",
                "product/new-balance/bag.webp",
                "https://www.nbkorea.com/product/bag");
        when(placeProductMapper.findProductImagesByNavigationKey("B2_STORE_0012", 6))
                .thenReturn(List.of(row));
        when(s3Provider.resolveImageUrl("product/new-balance/bag.webp"))
                .thenReturn("https://cdn.ditto/product/new-balance/bag.webp");

        PlaceProductImageResponse response =
                service.getProductImages("B2_STORE_0012", null).get(0);

        assertThat(response.getImageUrl())
                .isEqualTo("https://cdn.ditto/product/new-balance/bag.webp");
    }

    @Test
    @DisplayName("빈 navigationKey는 거절한다")
    void rejectsBlankNavigationKey() {
        assertThatThrownBy(() -> service.getProductImages(" ", null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("limit은 1 이상 20 이하만 허용한다")
    void validatesLimit() {
        assertThatThrownBy(() -> service.getProductImages("B2_STORE_0012", 0))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.getProductImages("B2_STORE_0012", 21))
                .isInstanceOf(BusinessException.class);
    }

    private PlaceProductImageRow row(
            Long productId,
            String productName,
            Long brandId,
            String brandName,
            String imageUrl,
            String productUrl) {
        PlaceProductImageRow row = new PlaceProductImageRow();
        row.setProductId(productId);
        row.setProductName(productName);
        row.setBrandId(brandId);
        row.setBrandName(brandName);
        row.setImageUrl(imageUrl);
        row.setProductUrl(productUrl);
        return row;
    }
}
