package com.ditto.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ditto.community.dto.response.BookmarkResponse;
import com.ditto.community.repository.PostBookmarkMapper;
import com.ditto.course.repository.PostMapper;
import com.ditto.course.repository.PostMapper.PostRow;
import com.ditto.global.common.response.PageResponse;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.user.dto.response.UserBookmarkResponse;

@ExtendWith(MockitoExtension.class)
class PostBookmarkServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 23L;

    @Mock
    private PostBookmarkMapper postBookmarkMapper;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostBookmarkService postBookmarkService;

    @Test
    @DisplayName("공개 코스 게시글을 정상적으로 북마크(저장)한다")
    void addBookmarkSuccess() {
        PostRow post = new PostRow(POST_ID, 23L, 22L, "제목", "내용", 77, 34, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));
        given(postBookmarkMapper.existsByPostIdAndUserId(POST_ID, USER_ID)).willReturn(false);
        given(postBookmarkMapper.insertBookmark(POST_ID, USER_ID)).willReturn(1);
        given(postMapper.incrementSaveCount(POST_ID)).willReturn(1);
        given(postBookmarkMapper.countByPostId(POST_ID)).willReturn(35);

        BookmarkResponse response = postBookmarkService.addBookmark(USER_ID, POST_ID);

        assertThat(response).isNotNull();
        assertThat(response.getPostId()).isEqualTo(POST_ID);
        assertThat(response.getBookmarkCount()).isEqualTo(35);
        assertThat(response.getIsBookmarked()).isTrue();

        verify(postBookmarkMapper).insertBookmark(POST_ID, USER_ID);
        verify(postMapper).incrementSaveCount(POST_ID);
    }

    @Test
    @DisplayName("이미 북마크한 게시글에 다시 북마크 요청 시 ALREADY_BOOKMARKED 예외가 발생한다")
    void rejectAddBookmarkWhenAlreadyBookmarked() {
        PostRow post = new PostRow(POST_ID, 23L, 22L, "제목", "내용", 77, 34, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));
        given(postBookmarkMapper.existsByPostIdAndUserId(POST_ID, USER_ID)).willReturn(true);

        assertThatThrownBy(() -> postBookmarkService.addBookmark(USER_ID, POST_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_BOOKMARKED);

        verify(postBookmarkMapper, never()).insertBookmark(any(), any());
        verify(postMapper, never()).incrementSaveCount(any());
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 북마크 요청 시 POST_NOT_FOUND 예외가 발생한다")
    void rejectAddBookmarkWhenPostNotFound() {
        given(postMapper.findActiveById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postBookmarkService.addBookmark(USER_ID, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(postBookmarkMapper, never()).insertBookmark(any(), any());
    }

    @Test
    @DisplayName("북마크를 취소하면 post_bookmark에서 삭제되고 save_count가 감소한다")
    void removeBookmarkSuccess() {
        PostRow post = new PostRow(POST_ID, 23L, 22L, "제목", "내용", 77, 35, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));
        given(postBookmarkMapper.existsByPostIdAndUserId(POST_ID, USER_ID)).willReturn(true);
        given(postBookmarkMapper.deleteBookmark(POST_ID, USER_ID)).willReturn(1);
        given(postMapper.decrementSaveCount(POST_ID)).willReturn(1);
        given(postBookmarkMapper.countByPostId(POST_ID)).willReturn(34);

        BookmarkResponse response = postBookmarkService.removeBookmark(USER_ID, POST_ID);

        assertThat(response).isNotNull();
        assertThat(response.getPostId()).isEqualTo(POST_ID);
        assertThat(response.getBookmarkCount()).isEqualTo(34);
        assertThat(response.getIsBookmarked()).isFalse();

        verify(postBookmarkMapper).deleteBookmark(POST_ID, USER_ID);
        verify(postMapper).decrementSaveCount(POST_ID);
    }

    @Test
    @DisplayName("내 북마크 목록을 페이징하여 정상 조회한다")
    void getMyBookmarksSuccess() {
        UserBookmarkResponse b1 = UserBookmarkResponse.builder()
                .postId(23L)
                .courseId(23L)
                .title("K-POP 팝업스토어 & 한식 맛집 코스")
                .likeCount(77L)
                .bookmarkCount(34L)
                .bookmarkedAt(LocalDateTime.now())
                .build();

        given(postBookmarkMapper.findBookmarksByUserId(USER_ID, 0L, 10)).willReturn(List.of(b1));
        given(postBookmarkMapper.countBookmarksByUserId(USER_ID)).willReturn(1L);

        PageResponse<UserBookmarkResponse> response = postBookmarkService.getMyBookmarks(USER_ID, 0, 10);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("K-POP 팝업스토어 & 한식 맛집 코스");
        assertThat(response.getTotalElements()).isEqualTo(1L);
    }
}
