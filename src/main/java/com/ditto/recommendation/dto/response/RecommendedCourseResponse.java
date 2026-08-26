package com.ditto.recommendation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 기본 추천 코스 목록의 한 줄.
 *
 * <p>목록 화면이 카드를 그리는 데 필요한 것까지만 담는다 — 상세를 열지 않고도 제목·설명·
 * 대표 사진·들르는 곳 이름이 보여야 한다. 자리별 추천 이유나 층 정보는 상세
 * ({@code GET /api/v1/courses/{courseId}})에 있다.
 */
@Getter
@Builder
public class RecommendedCourseResponse {

    private Long courseId;
    private String name;
    private String description;

    /** {@code COUNTRY.CODE} 와 같은 글자 (KR·JP·CN·US). 아직 안 정해진 코스는 null. */
    private String countryCode;

    private int placeCount;

    /** 카드에 칩으로 붙일 장소 이름. 앞에서부터 몇 개만 온다. */
    private List<String> placeNames;

    /** 첫 자리의 매장 사진. S3 키가 아니라 바로 쓸 수 있는 주소다. */
    private String imageUrl;

    private LocalDateTime createdAt;
}
