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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class PostCommentServiceTest {

    private static final Long USER_ID = 2L;
    private static final Long POST_ID = 10L;

    @Mock
    private PostCommentMapper postCommentMapper;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostCommentService postCommentService;

    @Test
    @DisplayName("코스 게시글에 정상적으로 댓글을 작성한다")
    void createCommentSuccess() {
        PostRow post = new PostRow(POST_ID, 100L, 1L, "게시글 제목", "게시글 내용", 0, 0, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));

        CommentResponse expectedResponse = CommentResponse.builder()
                .commentId(1L)
                .postId(POST_ID)
                .userId(USER_ID)
                .nickname("Chen_Li")
                .isAuthor(false)
                .content("오전에 가려면 몇 시쯤 도착하는 게 좋을까요?")
                .createdAt(LocalDateTime.now())
                .build();

        given(postCommentMapper.findCommentById(any())).willReturn(expectedResponse);

        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("  오전에 가려면 몇 시쯤 도착하는 게 좋을까요?  ")
                .build();

        CommentResponse response = postCommentService.createComment(USER_ID, POST_ID, request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("오전에 가려면 몇 시쯤 도착하는 게 좋을까요?");
        assertThat(response.getNickname()).isEqualTo("Chen_Li");
        assertThat(response.getIsAuthor()).isFalse();

        ArgumentCaptor<CommentInsertCommand> captor = ArgumentCaptor.forClass(CommentInsertCommand.class);
        verify(postCommentMapper).insert(captor.capture());
        assertThat(captor.getValue().getPostId()).isEqualTo(POST_ID);
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getContent()).isEqualTo("오전에 가려면 몇 시쯤 도착하는 게 좋을까요?");
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 작성 시 POST_NOT_FOUND 예외가 발생한다")
    void rejectCreateCommentWhenPostNotFound() {
        given(postMapper.findActiveById(999L)).willReturn(Optional.empty());

        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("댓글 내용")
                .build();

        assertThatThrownBy(() -> postCommentService.createComment(USER_ID, 999L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

        verify(postCommentMapper, never()).insert(any());
    }

    @Test
    @DisplayName("댓글 내용이 공백인 경우 INVALID_INPUT_VALUE 예외가 발생한다")
    void rejectCreateCommentWhenContentIsBlank() {
        PostRow post = new PostRow(POST_ID, 100L, 1L, "제목", "내용", 0, 0, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));

        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("   ")
                .build();

        assertThatThrownBy(() -> postCommentService.createComment(USER_ID, POST_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verify(postCommentMapper, never()).insert(any());
    }

    @Test
    @DisplayName("게시글의 댓글 목록을 시간 순서대로 조회한다")
    void getCommentsSuccess() {
        PostRow post = new PostRow(POST_ID, 100L, 1L, "제목", "내용", 0, 0, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));

        CommentResponse c1 = CommentResponse.builder()
                .commentId(1L)
                .postId(POST_ID)
                .userId(2L)
                .nickname("Chen_Li")
                .isAuthor(false)
                .content("워터폴 가든은 오전에 가면 사람이 적어서 사진 찍기 좋아요.")
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        CommentResponse c2 = CommentResponse.builder()
                .commentId(2L)
                .postId(POST_ID)
                .userId(1L) // 작성자
                .nickname("Yuki_T")
                .isAuthor(true)
                .content("10시 반 오픈이라 11시 전에 도착하면 한산해요.")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        given(postCommentMapper.findCommentsByPostId(POST_ID)).willReturn(List.of(c1, c2));

        List<CommentResponse> comments = postCommentService.getComments(POST_ID);

        assertThat(comments).hasSize(2);
        assertThat(comments.get(0).getNickname()).isEqualTo("Chen_Li");
        assertThat(comments.get(0).getIsAuthor()).isFalse();
        assertThat(comments.get(1).getNickname()).isEqualTo("Yuki_T");
        assertThat(comments.get(1).getIsAuthor()).isTrue();
    }

    @Test
    @DisplayName("작성자 본인이 본인의 댓글을 정상 수정한다")
    void updateCommentSuccess() {
        PostRow post = new PostRow(POST_ID, 100L, 1L, "제목", "내용", 0, 0, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));

        CommentResponse existingComment = CommentResponse.builder()
                .commentId(101L)
                .postId(POST_ID)
                .userId(USER_ID)
                .nickname("Chen_Li")
                .isAuthor(false)
                .content("이전 댓글 내용")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        CommentResponse updatedComment = CommentResponse.builder()
                .commentId(101L)
                .postId(POST_ID)
                .userId(USER_ID)
                .nickname("Chen_Li")
                .isAuthor(false)
                .content("수정된 댓글 내용")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        given(postCommentMapper.findCommentById(101L))
                .willReturn(existingComment)
                .willReturn(updatedComment);
        given(postCommentMapper.update(any(CommentUpdateCommand.class))).willReturn(1);

        UpdateCommentRequest request = UpdateCommentRequest.builder()
                .content("수정된 댓글 내용")
                .build();

        CommentResponse response = postCommentService.updateComment(USER_ID, POST_ID, 101L, request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("수정된 댓글 내용");

        ArgumentCaptor<CommentUpdateCommand> captor = ArgumentCaptor.forClass(CommentUpdateCommand.class);
        verify(postCommentMapper).update(captor.capture());
        assertThat(captor.getValue().getCommentId()).isEqualTo(101L);
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getContent()).isEqualTo("수정된 댓글 내용");
    }

    @Test
    @DisplayName("다른 사용자가 작성한 댓글을 수정하려고 하면 ACCESS_DENIED 예외가 발생한다")
    void rejectUpdateCommentWhenNotAuthor() {
        PostRow post = new PostRow(POST_ID, 100L, 1L, "제목", "내용", 0, 0, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));

        CommentResponse existingComment = CommentResponse.builder()
                .commentId(101L)
                .postId(POST_ID)
                .userId(99L) // 다른 사용자
                .nickname("OtherUser")
                .isAuthor(false)
                .content("다른 사용자의 댓글")
                .createdAt(LocalDateTime.now())
                .build();

        given(postCommentMapper.findCommentById(101L)).willReturn(existingComment);

        UpdateCommentRequest request = UpdateCommentRequest.builder()
                .content("수정 시도")
                .build();

        assertThatThrownBy(() -> postCommentService.updateComment(USER_ID, POST_ID, 101L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        verify(postCommentMapper, never()).update(any());
    }

    @Test
    @DisplayName("작성자 본인이 본인의 댓글을 정상 삭제한다")
    void deleteCommentSuccess() {
        PostRow post = new PostRow(POST_ID, 100L, 1L, "제목", "내용", 0, 0, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));

        CommentResponse comment = CommentResponse.builder()
                .commentId(101L)
                .postId(POST_ID)
                .userId(USER_ID)
                .nickname("Chen_Li")
                .isAuthor(false)
                .content("삭제할 댓글")
                .createdAt(LocalDateTime.now())
                .build();

        given(postCommentMapper.findCommentById(101L)).willReturn(comment);
        given(postCommentMapper.delete(101L, USER_ID)).willReturn(1);

        postCommentService.deleteComment(USER_ID, POST_ID, 101L);

        verify(postCommentMapper).delete(101L, USER_ID);
    }

    @Test
    @DisplayName("다른 사용자가 작성한 댓글을 삭제하려고 하면 ACCESS_DENIED 예외가 발생한다")
    void rejectDeleteCommentWhenNotAuthor() {
        PostRow post = new PostRow(POST_ID, 100L, 1L, "제목", "내용", 0, 0, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));

        CommentResponse comment = CommentResponse.builder()
                .commentId(101L)
                .postId(POST_ID)
                .userId(99L) // 다른 사용자
                .nickname("OtherUser")
                .isAuthor(false)
                .content("다른 사용자의 댓글")
                .createdAt(LocalDateTime.now())
                .build();

        given(postCommentMapper.findCommentById(101L)).willReturn(comment);

        assertThatThrownBy(() -> postCommentService.deleteComment(USER_ID, POST_ID, 101L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        verify(postCommentMapper, never()).delete(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 댓글 삭제 시 COMMENT_NOT_FOUND 예외가 발생한다")
    void rejectDeleteCommentWhenNotFound() {
        PostRow post = new PostRow(POST_ID, 100L, 1L, "제목", "내용", 0, 0, LocalDateTime.now(), null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(post));
        given(postCommentMapper.findCommentById(999L)).willReturn(null);

        assertThatThrownBy(() -> postCommentService.deleteComment(USER_ID, POST_ID, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);

        verify(postCommentMapper, never()).delete(any(), any());
    }
}
