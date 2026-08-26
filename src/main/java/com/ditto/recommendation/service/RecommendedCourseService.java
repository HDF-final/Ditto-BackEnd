package com.ditto.recommendation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.global.common.response.PageResponse;
import com.ditto.global.infrastructure.s3.S3Provider;
import com.ditto.recommendation.dto.response.RecommendedCourseResponse;
import com.ditto.recommendation.repository.RecommendedCourseMapper;

import lombok.RequiredArgsConstructor;

/**
 * 기본 추천 코스 — 손님이 보는 목록.
 *
 * <p>커뮤니티({@code /api/v1/community/courses})와 **갈라 둔 이유**가 있다. 커뮤니티는
 * 손님이 올린 글이라 좋아요·북마크·댓글이 붙고 작성자가 있지만, 기본 추천 코스는
 * 우리가 거는 콘텐츠라 그런 것이 없다. 한 목록에 섞으면 카드가 두 종류가 되고
 * 정렬 기준도 갈린다.
 */
@Service
@RequiredArgsConstructor
public class RecommendedCourseService {

    /** 카드에 붙일 장소 칩 수. 더 넣으면 카드가 두 줄로 접힌다. */
    private static final int PLACE_CHIPS = 3;

    private final RecommendedCourseMapper mapper;
    private final S3Provider s3Provider;

    /**
     * @param countryCode {@code COUNTRY.CODE} (KR·JP·CN·US). 비우면 나라를 안 보고 전부.
     */
    @Transactional(readOnly = true)
    public PageResponse<RecommendedCourseResponse> getRecommended(
            int page, int size, String countryCode) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String country = (countryCode == null || countryCode.isBlank())
                ? null : countryCode.trim().toUpperCase();

        long total = mapper.countRecommended(country);
        List<RecommendedCourseResponse> content = mapper
                .findRecommended(country, (long) safePage * safeSize, safeSize)
                .stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(content, safePage, total);
    }

    private RecommendedCourseResponse toResponse(RecommendedCourseMapper.CourseRow row) {
        List<String> names = mapper.findPlaceNames(row.getCourseId());
        return RecommendedCourseResponse.builder()
                .courseId(row.getCourseId())
                .name(row.getName())
                .description(row.getDescription())
                .countryCode(row.getCountryCode())
                .placeCount(row.getPlaceCount())
                .placeNames(names.size() > PLACE_CHIPS ? names.subList(0, PLACE_CHIPS) : names)
                // DB 에는 `place-picture/…` 처럼 키만 들어 있다. 화면이 그대로 <img src> 로
                // 쓸 수 있게 여기서 주소로 만든다 (`CourseService` 와 같은 방식).
                .imageUrl(s3Provider.resolveImageUrl(mapper.findLeadImageKey(row.getCourseId())))
                .createdAt(row.getCreatedAt())
                .build();
    }
}
