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

    void insert(PostImageInsertCommand command);

    int countByPostId(@Param("postId") Long postId);

    /** 새 사진을 이어 붙일 다음 정렬 값. 사진이 없으면 0. (post_id, sort_order) 유니크 충돌을 피한다. */
    int nextSortOrder(@Param("postId") Long postId);

    /** 게시글 하나의 사진 S3 key를 정렬 순서대로 조회한다. */
    List<String> findKeysByPostId(@Param("postId") Long postId);

    /** 목록 조회의 N+1을 피하기 위해 여러 게시글의 사진 key를 한 번에 조회한다. */
    List<PostImageKeyRow> findKeysByPostIds(@Param("postIds") List<Long> postIds);

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
