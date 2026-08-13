package com.ditto.course.repository;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ditto.course.domain.Course;
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

    int countByShareCode(@Param("shareCode") String shareCode);

    List<MyCourseSummaryResponse> findSummariesByUserId(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("size") int size);

    long countByUserId(@Param("userId") Long userId);

    int insert(CourseInsertCommand command);

    int insertPlace(CoursePlaceInsertCommand command);

    int softDelete(@Param("courseId") Long courseId);

    @Getter
    @Setter
    class CourseInsertCommand {
        private Long courseId;
        private Long userId;
        private Long sourceCourseId;
        private String name;
        private String description;
        private String creationType;
        private String shareCode;
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
