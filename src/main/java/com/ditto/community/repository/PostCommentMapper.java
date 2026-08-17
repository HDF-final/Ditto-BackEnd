package com.ditto.community.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ditto.community.dto.response.CommentResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Mapper
public interface PostCommentMapper {

    void insert(CommentInsertCommand command);

    List<CommentResponse> findCommentsByPostId(@Param("postId") Long postId);

    CommentResponse findCommentById(@Param("commentId") Long commentId);

    int delete(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CommentInsertCommand {
        private Long commentId;
        private Long postId;
        private Long userId;
        private String content;
        private LocalDateTime createdAt;
    }
}
