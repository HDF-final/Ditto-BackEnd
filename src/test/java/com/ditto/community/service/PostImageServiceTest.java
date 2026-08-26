package com.ditto.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.ditto.community.dto.response.PostImageUploadResponse;
import com.ditto.community.repository.PostImageMapper;
import com.ditto.community.repository.PostImageMapper.PostImageInsertCommand;
import com.ditto.course.repository.PostMapper;
import com.ditto.course.repository.PostMapper.PostRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.global.infrastructure.s3.S3Provider;
import com.ditto.global.infrastructure.s3.S3UploadResult;

@ExtendWith(MockitoExtension.class)
class PostImageServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long POST_ID = 2L;

    @Mock
    private PostMapper postMapper;

    @Mock
    private PostImageMapper postImageMapper;

    @Mock
    private S3Provider s3Provider;

    @InjectMocks
    private PostImageService postImageService;

    private PostRow ownPost() {
        return new PostRow(POST_ID, 100L, USER_ID, "제목", "내용", 3, 4, null, null);
    }

    private MultipartFile image(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    @Test
    @DisplayName("본인 게시글에 사진을 업로드하면 S3 key를 저장하고 조회 URL 목록을 반환한다")
    void uploadImagesSuccess() {
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(ownPost()));
        given(postImageMapper.countByPostId(POST_ID)).willReturn(1);
        given(postImageMapper.nextSortOrder(POST_ID)).willReturn(2);
        given(s3Provider.uploadImage(any(MultipartFile.class), eq("community/posts")))
                .willReturn(S3UploadResult.builder().key("images/community/posts/a.jpg").url("ignored").build(),
                        S3UploadResult.builder().key("images/community/posts/b.jpg").url("ignored").build());
        given(postImageMapper.findKeysByPostId(POST_ID))
                .willReturn(List.of("images/community/posts/old.jpg",
                        "images/community/posts/a.jpg",
                        "images/community/posts/b.jpg"));
        given(s3Provider.resolveDirectImageUrl(anyString()))
                .willAnswer(invocation -> "https://cdn/" + invocation.getArgument(0));

        PostImageUploadResponse response = postImageService.uploadImages(
                USER_ID, POST_ID, List.of(image("a"), image("b")));

        ArgumentCaptor<PostImageInsertCommand> captor = ArgumentCaptor.forClass(PostImageInsertCommand.class);
        verify(postImageMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        List<PostImageInsertCommand> saved = captor.getAllValues();
        // nextSortOrder(2)부터 이어 붙는다.
        assertThat(saved.get(0).getSortOrder()).isEqualTo(2);
        assertThat(saved.get(0).getImageKey()).isEqualTo("images/community/posts/a.jpg");
        assertThat(saved.get(1).getSortOrder()).isEqualTo(3);
        assertThat(saved.get(1).getImageKey()).isEqualTo("images/community/posts/b.jpg");

        assertThat(response.getPostId()).isEqualTo(POST_ID);
        assertThat(response.getImageUrls()).containsExactly(
                "https://cdn/images/community/posts/old.jpg",
                "https://cdn/images/community/posts/a.jpg",
                "https://cdn/images/community/posts/b.jpg");
    }

    @Test
    @DisplayName("빈 파일 목록이면 INVALID_IMAGE_FILE로 막고 S3를 부르지 않는다")
    void rejectsEmptyFileList() {
        assertThatThrownBy(() -> postImageService.uploadImages(USER_ID, POST_ID, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE_FILE);

        verify(s3Provider, never()).uploadImage(any(), anyString());
    }

    @Test
    @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND")
    void rejectsMissingPost() {
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postImageService.uploadImages(USER_ID, POST_ID, List.of(image("a"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_NOT_FOUND);

        verify(s3Provider, never()).uploadImage(any(), anyString());
    }

    @Test
    @DisplayName("작성자가 아니면 ACCESS_DENIED")
    void rejectsNonAuthor() {
        PostRow othersPost = new PostRow(POST_ID, 100L, 99L, "제목", "내용", 3, 4, null, null);
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(othersPost));

        assertThatThrownBy(() -> postImageService.uploadImages(USER_ID, POST_ID, List.of(image("a"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);

        verify(s3Provider, never()).uploadImage(any(), anyString());
    }

    @Test
    @DisplayName("기존 개수 + 신규가 최대치를 넘으면 POST_IMAGE_LIMIT_EXCEEDED")
    void rejectsOverLimit() {
        given(postMapper.findActiveById(POST_ID)).willReturn(Optional.of(ownPost()));
        given(postImageMapper.countByPostId(POST_ID)).willReturn(9);

        assertThatThrownBy(() -> postImageService.uploadImages(
                USER_ID, POST_ID, List.of(image("a"), image("b"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);

        verify(s3Provider, never()).uploadImage(any(), anyString());
    }
}
