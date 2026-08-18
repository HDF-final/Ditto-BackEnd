package com.ditto.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.community.dto.response.LikeResponse;
import com.ditto.community.repository.PostLikeMapper;
import com.ditto.course.repository.PostMapper;
import com.ditto.course.repository.PostMapper.PostRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 23L;

    @Mock
    private PostLikeMapper postLikeMapper;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostLikeService postLikeService;

    @Test
    @DisplayName("공개 코스 게시글에 정상적으로 좋아요를 등록한다")
    void addLikeSuccess() {
        PostRow post = new PostRow(POST_ID, 23L, 22L, "제목", "내용", 77, 34, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));
        given(postLikeMapper.existsByPostIdAndUserId(POST_ID, USER_ID)).willReturn(false);
        given(postLikeMapper.insertLike(POST_ID, USER_ID)).willReturn(1);
        given(postMapper.incrementLikesCount(POST_ID)).willReturn(1);
        given(postLikeMapper.countByPostId(POST_ID)).willReturn(78);

        LikeResponse response = postLikeService.addLike(USER_ID, POST_ID);

        assertThat(response).isNotNull();
        assertThat(response.getPostId()).isEqualTo(POST_ID);
        assertThat(response.getLikesCount()).isEqualTo(78);
        assertThat(response.getIsLiked()).isTrue();

        verify(postLikeMapper).insertLike(POST_ID, USER_ID);
        verify(postMapper).incrementLikesCount(POST_ID);
    }

    @Test
    @DisplayName("이미 좋아요를 누른 게시글에 다시 좋아요 요청 시 ALREADY_LIKED 예외가 발생한다")
    void rejectAddLikeWhenAlreadyLiked() {
        PostRow post = new PostRow(POST_ID, 23L, 22L, "제목", "내용", 77, 34, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));
        given(postLikeMapper.existsByPostIdAndUserId(POST_ID, USER_ID)).willReturn(true);

        assertThatThrownBy(() -> postLikeService.addLike(USER_ID, POST_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_LIKED);

        verify(postLikeMapper, never()).insertLike(any(), any());
        verify(postMapper, never()).incrementLikesCount(any());
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 좋아요 요청 시 POST_NOT_FOUND 예외가 발생한다")
    void rejectAddLikeWhenPostNotFound() {
        given(postMapper.findActiveById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postLikeService.addLike(USER_ID, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(postLikeMapper, never()).insertLike(any(), any());
    }

    @Test
    @DisplayName("좋아요를 취소하면 post_like에서 삭제되고 likes_count가 감소한다")
    void removeLikeSuccess() {
        PostRow post = new PostRow(POST_ID, 23L, 22L, "제목", "내용", 78, 34, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));
        given(postLikeMapper.existsByPostIdAndUserId(POST_ID, USER_ID)).willReturn(true);
        given(postLikeMapper.deleteLike(POST_ID, USER_ID)).willReturn(1);
        given(postMapper.decrementLikesCount(POST_ID)).willReturn(1);
        given(postLikeMapper.countByPostId(POST_ID)).willReturn(77);

        LikeResponse response = postLikeService.removeLike(USER_ID, POST_ID);

        assertThat(response).isNotNull();
        assertThat(response.getPostId()).isEqualTo(POST_ID);
        assertThat(response.getLikesCount()).isEqualTo(77);
        assertThat(response.getIsLiked()).isFalse();

        verify(postLikeMapper).deleteLike(POST_ID, USER_ID);
        verify(postMapper).decrementLikesCount(POST_ID);
    }
}
