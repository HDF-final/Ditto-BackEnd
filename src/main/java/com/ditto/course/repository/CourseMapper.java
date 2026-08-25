package com.ditto.course.repository;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ditto.course.domain.Course;
import com.ditto.course.dto.response.CoursePlaceResponse;
import com.ditto.course.dto.response.MyCourseSummaryResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * course·course_place 테이블 매퍼. SQL 은 resources/mapper/CourseMapper.xml 에 있다.
 */
@Mapper
public interface CourseMapper {

    Optional<Course> findById(@Param("courseId") Long courseId);

    int countPlaceInCourse(
            @Param("courseId") Long courseId,
            @Param("placeId") Long placeId);

    int findMaxVisitOrder(@Param("courseId") Long courseId);

    int markVisitOrdersForShift(
            @Param("courseId") Long courseId,
            @Param("position") int position);

    int incrementMarkedVisitOrders(@Param("courseId") Long courseId);

    Optional<Integer> findVisitOrderByCourseAndPlace(
            @Param("courseId") Long courseId,
            @Param("placeId") Long placeId);

    int deletePlace(
            @Param("courseId") Long courseId,
            @Param("placeId") Long placeId);

    int markVisitOrdersAfterDeleted(
            @Param("courseId") Long courseId,
            @Param("deletedVisitOrder") int deletedVisitOrder);

    int decrementMarkedVisitOrders(@Param("courseId") Long courseId);

    List<MyCourseSummaryResponse> findSummariesByUserId(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("size") int size);

    long countByUserId(@Param("userId") Long userId);

    List<CoursePlaceResponse> findPlacesByCourseId(@Param("courseId") Long courseId);

    int insert(CourseInsertCommand command);

    int insertPlace(CoursePlaceInsertCommand command);

    int copyPlacesFromCourse(
            @Param("sourceCourseId") Long sourceCourseId,
            @Param("createdCourseId") Long createdCourseId,
            @Param("visitStatus") String visitStatus);

    boolean existsPublicPostByCourseId(@Param("courseId") Long courseId);

    int softDelete(@Param("courseId") Long courseId);

    int updateInfo(@Param("courseId") Long courseId,
                   @Param("name") String name,
                   @Param("description") String description);

    List<Long> findPlaceIdsByCourseId(@Param("courseId") Long courseId);

    int markVisitOrdersForReorder(@Param("courseId") Long courseId);

    int reorderPlaces(@Param("courseId") Long courseId,
                      @Param("orderedPlaceIds") List<Long> orderedPlaceIds);

    @Getter
    @Setter
    class CourseInsertCommand {
        private Long courseId;
        private Long userId;
        private Long sourceCourseId;
        private String name;
        private String description;
        private String creationType;
    }

    @Getter
    @AllArgsConstructor
    class CoursePlaceInsertCommand {
        private Long courseId;
        private Long placeId;
        private int visitOrder;
        private String recommendationReason;
        private String visitStatus;
    }
}
