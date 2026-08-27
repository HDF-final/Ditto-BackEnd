package com.ditto.recommendation.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import lombok.Getter;
import lombok.Setter;

/**
 * 기본 추천 코스({@code course.creation_type = 'SYSTEM'}) 매퍼.
 *
 * <p>SQL 은 resources/mapper/RecommendedCourseMapper.xml 에 있다. 손님용 목록과 관리자용
 * 목록·수정·삭제가 같은 행을 보므로 한 매퍼에 둔다.
 */
@Mapper
public interface RecommendedCourseMapper {

    /** 손님용 목록. 나라를 안 주면 전부(나라 미지정 코스 포함). */
    List<CourseRow> findRecommended(
            @Param("countryCode") String countryCode,
            @Param("offset") long offset,
            @Param("size") int size);

    long countRecommended(@Param("countryCode") String countryCode);

    /** 관리자용 목록. 페이지를 안 자른다 — 지금 걸려 있는 것을 다 봐야 한다. */
    List<AdminCourseRow> findAllForAdmin();

    AdminCourseRow findOneForAdmin(@Param("courseId") Long courseId);

    List<PlaceRow> findPlaces(@Param("courseId") Long courseId);

    /** 자리 이름만. 목록 카드의 칩에 쓴다. */
    List<String> findPlaceNames(@Param("courseId") Long courseId);

    /**
     * 이름·설명을 한 번에. {@code CourseMapper.updateInfo} 를 안 쓰는 것은 여기서
     * SYSTEM 이 아닌 코스를 못 건드리게 막아야 하기 때문이다.
     *
     * <p>나라는 여기서 안 고친다 — {@code COURSE_COUNTRY} 를 아래 둘이 간다.
     */
    int updateInfo(@Param("courseId") Long courseId,
                   @Param("name") String name,
                   @Param("description") String description);

    /** 이 코스에 걸린 나라를 전부 뗀다. 갈아 끼우기의 앞 절반이다. */
    int deleteCountries(@Param("courseId") Long courseId);

    /** 나라 하나를 건다. {@code COUNTRY.CODE} 에 FK 가 걸려 있어 없는 코드는 안 들어간다. */
    int insertCountry(@Param("courseId") Long courseId,
                      @Param("countryCode") String countryCode);

    /** 대표 사진 키. <b>null 이 "기본값으로 되돌린다"</b> 는 뜻이다. */
    int updateMainImage(@Param("courseId") Long courseId,
                        @Param("mainImage") String mainImage);

    /** 자리 하나의 추천 이유. 자리 구성은 안 건드린다. */
    int updatePlaceReason(@Param("courseId") Long courseId,
                          @Param("placeId") Long placeId,
                          @Param("recommendationReason") String recommendationReason);

    /** 게시글 본문. 작성자를 안 본다 — SYSTEM 코스의 글은 주인이 없다(user_id 는 시스템 계정). */
    int updatePostContent(@Param("courseId") Long courseId,
                          @Param("content") String content);

    int softDelete(@Param("courseId") Long courseId);

    /** 코스를 내리면 붙어 있던 게시글도 같이 내린다. */
    int softDeletePost(@Param("courseId") Long courseId);

    @Getter
    @Setter
    class CourseRow {
        private Long courseId;
        private String name;
        private String description;
        /** `KR,JP` — LISTAGG 로 모아 온 것. 받는 쪽이 쉼표로 가른다. */
        private String countryCodes;
        private int placeCount;
        private LocalDateTime createdAt;
        /** 대표 사진 키. 관리자가 지정한 것 → 셀럽 사진 → 첫 자리 매장 사진 차례. */
        private String heroImageKey;
    }

    @Getter
    @Setter
    class AdminCourseRow {
        private Long courseId;
        private String name;
        private String description;
        /** `KR,JP` — LISTAGG 로 모아 온 것. 받는 쪽이 쉼표로 가른다. */
        private String countryCodes;
        private String shareCode;
        private int placeCount;
        private Long postId;
        private String postContent;
        private int imageCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        /** 대표 사진 키. 관리자가 지정한 것 → 셀럽 사진 → 첫 자리 매장 사진 차례. */
        private String heroImageKey;

        /**
         * 관리자가 <b>직접 지정한</b> 대표 사진 키. 안 지정했으면 null 이고, 그때
         * {@code heroImageKey} 는 기본값으로 채워져 온다. 편집기가 "기본값을 쓰는 중" 인지
         * 알아야 '기본값으로 되돌리기' 를 그릴 수 있어 둘 다 준다.
         */
        private String mainImage;
    }

    @Getter
    @Setter
    class PlaceRow {
        private Long placeId;
        private String name;
        private String imageUrl;
        private String floorCode;
        private int visitOrder;
        private String recommendationReason;
    }
}
