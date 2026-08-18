package com.ditto.community.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.community.dto.request.CreateCommentRequest;
import com.ditto.community.dto.request.UpdateCommentRequest;
import com.ditto.community.dto.response.CommentResponse;
import com.ditto.community.repository.PostCommentMapper;
import com.ditto.community.repository.PostCommentMapper.CommentInsertCommand;
import com.ditto.community.repository.PostCommentMapper.CommentUpdateCommand;
import com.ditto.course.repository.PostMapper;
import com.ditto.course.repository.PostMapper.PostRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCommentService {

    private final PostCommentMapper postCommentMapper;
    private final PostMapper postMapper;

    /**
     * 특정 코스 게시글에 댓글을 작성한다. (ROLE_CUSTOMER 전용)
     */
    @Transactional
    public CommentResponse createComment(Long userId, Long postId, CreateCommentRequest request) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 게시글 존재 및 삭제 여부 확인
        PostRow post = postMapper.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        String content = request.getContent() != null ? request.getContent().trim() : "";
        if (content.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        CommentInsertCommand command = CommentInsertCommand.builder()
                .postId(postId)
                .userId(userId)
                .content(content)
                .build();

        postCommentMapper.insert(command);

        return postCommentMapper.findCommentById(command.getCommentId());
    }

    /**
     * 특정 코스 게시글의 댓글 목록을 조회한다.
     */
    public List<CommentResponse> getComments(Long postId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        postMapper.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        List<CommentResponse> comments = postCommentMapper.findCommentsByPostId(postId);
        return comments != null ? comments : List.of();
    }

    /**
     * 본인이 작성한 댓글을 수정한다. (ROLE_CUSTOMER 전용)
     */
    @Transactional
    public CommentResponse updateComment(Long userId, Long postId, Long commentId, UpdateCommentRequest request) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (commentId == null || commentId <= 0) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        String content = request.getContent() != null ? request.getContent().trim() : "";
        if (content.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 1. 게시글 존재 여부 확인
        postMapper.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 2. 댓글 존재 여부 확인
        CommentResponse comment = postCommentMapper.findCommentById(commentId);
        if (comment == null || !comment.getPostId().equals(postId)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 3. 본인 작성 댓글인지 확인
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 4. 수정 실행
        CommentUpdateCommand command = CommentUpdateCommand.builder()
                .commentId(commentId)
                .userId(userId)
                .content(content)
                .build();

        if (postCommentMapper.update(command) != 1) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        return postCommentMapper.findCommentById(commentId);
    }

    /**
     * 본인이 작성한 댓글을 삭제한다. (ROLE_CUSTOMER 전용)
     */
    @Transactional
    public void deleteComment(Long userId, Long postId, Long commentId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (commentId == null || commentId <= 0) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 1. 게시글 존재 여부 확인
        postMapper.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 2. 댓글 존재 여부 확인
        CommentResponse comment = postCommentMapper.findCommentById(commentId);
        if (comment == null || !comment.getPostId().equals(postId)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 3. 본인 작성 댓글인지 확인
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 4. 삭제 실행
        if (postCommentMapper.delete(commentId, userId) != 1) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
    }
}
