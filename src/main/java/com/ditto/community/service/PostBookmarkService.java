package com.ditto.community.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.community.dto.response.BookmarkResponse;
import com.ditto.community.repository.PostBookmarkMapper;
import com.ditto.course.repository.PostMapper;
import com.ditto.course.repository.PostMapper.PostRow;
import com.ditto.global.common.response.PageResponse;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.user.dto.response.UserBookmarkResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostBookmarkService {

    private final PostBookmarkMapper postBookmarkMapper;
    private final PostMapper postMapper;

    /**
     * 공개 코스 게시글을 북마크에 등록한다. (ROLE_CUSTOMER 전용)
     */
    @Transactional
    public BookmarkResponse addBookmark(Long userId, Long postId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 1. 게시글 존재 및 삭제 여부 확인
        PostRow post = postMapper.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 2. 이미 북마크했는지 확인
        if (postBookmarkMapper.existsByPostIdAndUserId(postId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_BOOKMARKED);
        }

        // 3. 북마크 기록 저장 및 게시글 save_count 증가
        postBookmarkMapper.insertBookmark(postId, userId);
        postMapper.incrementSaveCount(postId);

        int updatedBookmarkCount = postBookmarkMapper.countByPostId(postId);

        return BookmarkResponse.builder()
                .postId(postId)
                .bookmarkCount(updatedBookmarkCount)
                .isBookmarked(true)
                .build();
    }

    /**
     * 공개 코스 게시글 북마크를 취소한다. (ROLE_CUSTOMER 전용)
     */
    @Transactional
    public BookmarkResponse removeBookmark(Long userId, Long postId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 1. 게시글 존재 및 삭제 여부 확인
        PostRow post = postMapper.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 2. 북마크 여부 확인 및 삭제
        if (postBookmarkMapper.existsByPostIdAndUserId(postId, userId)) {
            postBookmarkMapper.deleteBookmark(postId, userId);
            postMapper.decrementSaveCount(postId);
        }

        int updatedBookmarkCount = postBookmarkMapper.countByPostId(postId);

        return BookmarkResponse.builder()
                .postId(postId)
                .bookmarkCount(updatedBookmarkCount)
                .isBookmarked(false)
                .build();
    }

    /**
     * 현재 로그인한 사용자의 북마크 코스 목록을 조회한다. (ROLE_CUSTOMER 전용)
     */
    public PageResponse<UserBookmarkResponse> getMyBookmarks(Long userId, int page, int size) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        int validSize = (size <= 0 || size > 100) ? 10 : size;
        long offset = (long) page * validSize;

        List<UserBookmarkResponse> items = postBookmarkMapper.findBookmarksByUserId(userId, offset, validSize);
        long totalElements = postBookmarkMapper.countBookmarksByUserId(userId);

        return new PageResponse<>(items != null ? items : List.of(), page, totalElements);
    }
}
