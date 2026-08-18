package com.ditto.community.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.community.dto.response.LikeResponse;
import com.ditto.community.repository.PostLikeMapper;
import com.ditto.course.repository.PostMapper;
import com.ditto.course.repository.PostMapper.PostRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeMapper postLikeMapper;
    private final PostMapper postMapper;

    /**
     * 공개 코스 게시글에 좋아요를 등록한다. (ROLE_CUSTOMER 전용)
     */
    @Transactional
    public LikeResponse addLike(Long userId, Long postId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 1. 게시글 존재 및 삭제 여부 확인
        PostRow post = postMapper.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 2. 이미 좋아요를 눌렀는지 확인
        if (postLikeMapper.existsByPostIdAndUserId(postId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        // 3. 좋아요 기록 저장 및 게시글 likes_count 증가
        postLikeMapper.insertLike(postId, userId);
        postMapper.incrementLikesCount(postId);

        int updatedLikesCount = postLikeMapper.countByPostId(postId);

        return LikeResponse.builder()
                .postId(postId)
                .likesCount(updatedLikesCount)
                .isLiked(true)
                .build();
    }

    /**
     * 공개 코스 게시글의 좋아요를 취소한다. (ROLE_CUSTOMER 전용)
     */
    @Transactional
    public LikeResponse removeLike(Long userId, Long postId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 1. 게시글 존재 및 삭제 여부 확인
        PostRow post = postMapper.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 2. 좋아요 여부 확인 및 삭제
        if (postLikeMapper.existsByPostIdAndUserId(postId, userId)) {
            postLikeMapper.deleteLike(postId, userId);
            postMapper.decrementLikesCount(postId);
        }

        int updatedLikesCount = postLikeMapper.countByPostId(postId);

        return LikeResponse.builder()
                .postId(postId)
                .likesCount(updatedLikesCount)
                .isLiked(false)
                .build();
    }
}
