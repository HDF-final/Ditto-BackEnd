package com.ditto.community.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Mapper
public interface PostImageMapper {

    List<PostImageRow> findByPostId(@Param("postId") Long postId);

    List<PostImageRow> findByPostIdAndIds(
            @Param("postId") Long postId,
            @Param("imageIds") List<Long> imageIds);

    int countByPostId(@Param("postId") Long postId);

    int nextSortOrder(@Param("postId") Long postId);

    List<String> findKeysByPostId(@Param("postId") Long postId);

    List<PostImageKeyRow> findKeysByPostIds(@Param("postIds") List<Long> postIds);

    void insert(PostImageInsertCommand command);

    int deleteByPostId(@Param("postId") Long postId);

    int deleteByIds(
            @Param("postId") Long postId,
            @Param("imageIds") List<Long> imageIds);

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    class PostImageRow {
        private Long postImageId;
        private Long postId;
        private String imageKey;
        private Integer sortOrder;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class PostImageInsertCommand {
        private Long postImageId;
        private Long postId;
        private String imageKey;
        private int sortOrder;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    class PostImageKeyRow {
        private Long postId;
        private String imageKey;
    }
}
