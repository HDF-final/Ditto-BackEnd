package com.ditto.course.domain;

import java.time.LocalDateTime;

import lombok.Getter;

/**
 * {@code course} 테이블 매핑. 컨트롤러로 직접 노출하지 않는다.
 */
@Getter
public class Course {

    private Long courseId;
    private Long userId;
    private Long sourceCourseId;
    private String name;
    private String description;
    private String creationType;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public boolean isOwnedBy(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    /** insert 직후 또는 테스트에서 도메인 객체를 복원할 때 사용한다. */
    public static Course of(
            Long courseId,
            Long userId,
            Long sourceCourseId,
            String name,
            String description,
            String creationType) {
        Course course = new Course();
        course.courseId = courseId;
        course.userId = userId;
        course.sourceCourseId = sourceCourseId;
        course.name = name;
        course.description = description;
        course.creationType = creationType;
        return course;
    }
}
