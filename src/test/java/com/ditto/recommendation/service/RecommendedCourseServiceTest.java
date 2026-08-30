package com.ditto.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.global.common.response.PageResponse;
import com.ditto.global.i18n.ContentLanguage;
import com.ditto.global.infrastructure.s3.S3Provider;
import com.ditto.global.infrastructure.translation.ContentTranslationService;
import com.ditto.recommendation.dto.response.RecommendedCourseResponse;
import com.ditto.recommendation.repository.RecommendedCourseMapper;
import com.ditto.recommendation.repository.RecommendedCourseMapper.CourseRow;
import com.ditto.recommendation.repository.RecommendedCourseMapper.PlaceRow;

@ExtendWith(MockitoExtension.class)
class RecommendedCourseServiceTest {

    @Mock
    private RecommendedCourseMapper mapper;

    @Mock
    private S3Provider s3Provider;

    @Mock
    private ContentTranslationService contentTranslationService;

    @InjectMocks
    private RecommendedCourseService service;

    @Test
    void localizesCourseAndVisiblePlaceNames() {
        CourseRow course = courseRow();
        PlaceRow first = placeRow(11L, "Tamburins");
        PlaceRow second = placeRow(12L, "Gentle Monster");
        PlaceRow third = placeRow(13L, "Tiffany");
        PlaceRow hidden = placeRow(14L, "Hidden");

        given(mapper.countRecommended("JP")).willReturn(1L);
        given(mapper.findRecommended("JP", 0L, 20)).willReturn(List.of(course));
        given(mapper.findPlaces(7L)).willReturn(List.of(first, second, third, hidden));
        given(s3Provider.resolveImageUrlByPrefix("course/7.jpg"))
                .willReturn("https://cdn.example.com/course/7.jpg");
        given(contentTranslationService.translate(
                "course", "7", "name", "Brand course", ContentLanguage.JAPANESE))
                .willReturn("ブランドコース");
        given(contentTranslationService.translate(
                "course", "7", "description", "Description", ContentLanguage.JAPANESE))
                .willReturn("説明");
        given(contentTranslationService.translate(
                "place", "11", "name", "Tamburins", ContentLanguage.JAPANESE))
                .willReturn("タンバリンズ");
        given(contentTranslationService.translate(
                "place", "12", "name", "Gentle Monster", ContentLanguage.JAPANESE))
                .willReturn("ジェントルモンスター");
        given(contentTranslationService.translate(
                "place", "13", "name", "Tiffany", ContentLanguage.JAPANESE))
                .willReturn("ティファニー");

        PageResponse<RecommendedCourseResponse> response =
                service.getRecommended(0, 20, "jp", ContentLanguage.JAPANESE);

        assertThat(response.getTotalElements()).isEqualTo(1);
        RecommendedCourseResponse item = response.getContent().get(0);
        assertThat(item.getName()).isEqualTo("ブランドコース");
        assertThat(item.getDescription()).isEqualTo("説明");
        assertThat(item.getCountryCodes()).containsExactly("KR", "JP");
        assertThat(item.getPlaceNames())
                .containsExactly("タンバリンズ", "ジェントルモンスター", "ティファニー");
        assertThat(item.getImageUrl()).isEqualTo("https://cdn.example.com/course/7.jpg");
        verify(mapper).findRecommended("JP", 0L, 20);
    }

    @Test
    void keepsKoreanSourceWithoutTranslationCalls() {
        CourseRow course = courseRow();
        given(mapper.countRecommended(null)).willReturn(1L);
        given(mapper.findRecommended(null, 0L, 20)).willReturn(List.of(course));
        given(mapper.findPlaces(7L)).willReturn(List.of(placeRow(11L, "Tamburins")));
        given(s3Provider.resolveImageUrlByPrefix("course/7.jpg"))
                .willReturn("https://cdn.example.com/course/7.jpg");

        RecommendedCourseResponse item =
                service.getRecommended(0, 20, null).getContent().get(0);

        assertThat(item.getName()).isEqualTo("Brand course");
        assertThat(item.getDescription()).isEqualTo("Description");
        assertThat(item.getPlaceNames()).containsExactly("Tamburins");
        verifyNoInteractions(contentTranslationService);
    }

    private static CourseRow courseRow() {
        CourseRow row = new CourseRow();
        row.setCourseId(7L);
        row.setName("Brand course");
        row.setDescription("Description");
        row.setCountryCodes("KR,JP");
        row.setPlaceCount(4);
        row.setHeroImageKey("course/7.jpg");
        return row;
    }

    private static PlaceRow placeRow(Long placeId, String name) {
        PlaceRow row = new PlaceRow();
        row.setPlaceId(placeId);
        row.setName(name);
        return row;
    }
}
