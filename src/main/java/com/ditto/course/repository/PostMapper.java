package com.ditto.course.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ditto.community.dto.response.PublicCourseResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Mapper
public interface PostMapper {

    int insert(PostInsertCommand command);

    Optional<PostRow> findActiveById(Long postId);

    int update(PostUpdateCommand command);

    int softDelete(@Param("postId") Long postId, @Param("userId") Long userId);

    default List<PublicCourseResponse> findPublicCourses(long offset, int size) {
        return findPublicCourses(offset, size, null, null);
    }

    List<PublicCourseResponse> findPublicCourses(
            @Param("offset") long offset,
            @Param("size") int size,
            @Param("authorId") Long authorId,
            @Param("author") String author);

    default long countPublicCourses() {
        return countPublicCourses(null, null);
    }

    long countPublicCourses(
            @Param("authorId") Long authorId,
            @Param("author") String author);

    Optional<PublicCourseDetailPostRow> findPublicCourseDetailById(@Param("postId") Long postId);

    int incrementLikesCount(@Param("postId") Long postId);

    int decrementLikesCount(@Param("postId") Long postId);

    int incrementSaveCount(@Param("postId") Long postId);

    int decrementSaveCount(@Param("postId") Long postId);

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    class PublicCourseDetailPostRow {
        public PublicCourseDetailPostRow(Long postId, Long courseId, String title, String content) {
            this.postId = postId;
            this.courseId = courseId;
            this.title = title;
            this.content = content;
        }

        private Long postId;
        private Long courseId;
        private Long writerId;
        private String writerNickname;
        private String country;
        private String title;
        private String content;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class PostInsertCommand {
        private Long postId;
        private Long courseId;
        private Long userId;
        private String title;
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    class PostRow {
        private Long postId;
        private Long courseId;
        private Long userId;
        private String title;
        private String content;
        private Integer likesCount;
        private Integer saveCount;
        private LocalDateTime createdAt;
        private LocalDateTime deletedAt;

        public boolean isWrittenBy(Long userId) {
            return this.userId != null && this.userId.equals(userId);
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class PostUpdateCommand {
        private Long postId;
        private Long userId;
        private String title;
        private String content;
    }
}
