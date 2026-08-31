package com.ditto.course.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ditto.user.dto.response.UserSavedCourseResponse;

@Mapper
public interface CourseBookmarkMapper {

    int insertBookmark(
            @Param("courseId") Long courseId,
            @Param("userId") Long userId);

    int deleteBookmark(
            @Param("courseId") Long courseId,
            @Param("userId") Long userId);

    boolean existsByCourseIdAndUserId(
            @Param("courseId") Long courseId,
            @Param("userId") Long userId);

    List<UserSavedCourseResponse> findSavedCoursesByUserId(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("size") int size);

    long countSavedCoursesByUserId(@Param("userId") Long userId);
}
