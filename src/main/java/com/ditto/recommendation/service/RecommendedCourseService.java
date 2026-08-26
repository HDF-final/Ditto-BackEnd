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
                .countryCodes(splitCountries(row.getCountryCodes()))
                .placeCount(row.getPlaceCount())
                .placeNames(names.size() > PLACE_CHIPS ? names.subList(0, PLACE_CHIPS) : names)
                // DB 에는 `place-picture/…` 처럼 키만 들어 있다. 화면이 그대로 <img src> 로
                // 쓸 수 있게 여기서 주소로 만든다 (`CourseService` 와 같은 방식).
                // **어드민 카드와 같은 사진이다.** 관리자가 지정한 것 → 셀럽 사진 →
                // 첫 자리 매장 사진 차례로 목록 SQL 이 이미 골라 준다(heroImageKey).
                // 코스마다 findLeadImageKey 를 부르던 것을 걷어냈다 — 왕복이 코스 수만큼 들었다.
                .imageUrl(heroUrl(row.getHeroImageKey()))
                .createdAt(row.getCreatedAt())
                .build();
    }

    /**
     * {@code 'KR,JP'} → {@code [KR, JP]}. 값이 없으면 빈 목록이다.
     *
     * <p>한 칸에 쉼표로 담는 것은 나라가 넷뿐이고 SYSTEM 코스가 열몇 건이라 조인 표를
     * 두는 값이 안 들기 때문이다 (sql/course_country.sql 에 그 판단이 적혀 있다).
     */
    private static List<String> splitCountries(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(code -> code.toUpperCase(java.util.Locale.ROOT))
                .distinct()
                .toList();
    }

    /**
     * 대표 사진 키 → 주소. 셀럽 사진만 CDN 을 건너뛴다.
     *
     * <p>CloudFront 배포에 걸린 동작이 {@code brand-logo/*} · {@code place-picture/*} ·
     * {@code products/*} · {@code course-resource/*} 넷뿐이고 기본 동작은 ALB 라,
     * {@code course/*} 를 CDN 주소로 만들면 301 로 튕겨 사진이 안 뜬다 — 실측이다.
     *
     * <p>판단은 {@link S3Provider#resolveImageUrlByPrefix} 로 옮겼다. 코스 상세도 자리
     * 사진에 같은 규칙을 써야 하는데, 같은 규칙을 세 군데에 적어 두면 한 군데만
     * 고쳐지는 날이 온다.
     */
    private String heroUrl(String key) {
        return s3Provider.resolveImageUrlByPrefix(key);
    }
}
