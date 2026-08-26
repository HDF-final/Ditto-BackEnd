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

    /** 첫 자리의 매장 사진 키. 없으면 null. */
    String findLeadImageKey(@Param("courseId") Long courseId);

    /**
     * 이름·설명·나라를 한 번에. {@code CourseMapper.updateInfo} 를 안 쓰는 것은 그쪽이
     * 나라 칸을 모르고, 여기서 SYSTEM 이 아닌 코스를 못 건드리게 막아야 하기 때문이다.
     */
    int updateInfo(@Param("courseId") Long courseId,
                   @Param("name") String name,
                   @Param("description") String description,
                   @Param("countryCode") String countryCode);

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
        private String countryCode;
        private int placeCount;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    class AdminCourseRow {
        private Long courseId;
        private String name;
        private String description;
        private String countryCode;
        private String shareCode;
        private int placeCount;
        private Long postId;
        private String postContent;
        private int imageCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        /** 목록 카드의 대표 사진 키(첫 자리의 매장 사진). 자리가 없으면 null. */
        private String heroImageKey;
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
