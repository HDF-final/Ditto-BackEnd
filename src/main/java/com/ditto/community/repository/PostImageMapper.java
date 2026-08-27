package com.ditto.community.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Mapper
public interface PostImageMapper {

    List<PostImageRow> findByPostId(@Param("postId") Long postId);

    List<PostImageRow> findByPostIdAndIds(
            @Param("postId") Long postId,
            @Param("imageIds") List<Long> imageIds);

    Integer findMaxSortOrder(@Param("postId") Long postId);

    int insert(PostImageInsertCommand command);

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
        private String objectKey;
        private Integer sortOrder;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    class PostImageInsertCommand {
        private Long postImageId;
        private Long postId;
        private String objectKey;
        private Integer sortOrder;
    }
}
