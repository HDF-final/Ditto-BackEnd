package com.ditto.community.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.community.dto.request.CreateCoursePostRequest;
import com.ditto.community.dto.request.UpdateCoursePostRequest;
import com.ditto.community.dto.response.CreateCoursePostResponse;
import com.ditto.community.dto.response.PublicCourseDetailResponse;
import com.ditto.community.dto.response.PublicCourseResponse;
import com.ditto.community.dto.response.UpdateCoursePostResponse;
import com.ditto.course.domain.Course;
import com.ditto.course.dto.response.CoursePlaceResponse;
import com.ditto.course.repository.CourseMapper;
import com.ditto.course.repository.PostMapper;
import com.ditto.course.repository.PostMapper.PostInsertCommand;
import com.ditto.course.repository.PostMapper.PostRow;
import com.ditto.course.repository.PostMapper.PostUpdateCommand;
import com.ditto.course.repository.PostMapper.PublicCourseDetailPostRow;
import com.ditto.global.common.response.PageResponse;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.global.i18n.ContentLanguage;
import com.ditto.global.infrastructure.translation.ContentTranslationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CourseMapper courseMapper;
    private final PostMapper postMapper;
    private final com.ditto.community.repository.PostCommentMapper postCommentMapper;
    private final ContentTranslationService contentTranslationService;

    /**
     * 커뮤니티에 공개된 코스 게시글 목록을 최신순으로 페이징 조회한다.
     */
    public PageResponse<PublicCourseResponse> getPublicCourses(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        long offset = (long) page * size;
        List<PublicCourseResponse> content = postMapper.findPublicCourses(offset, size);
        long totalElements = postMapper.countPublicCourses();

        return new PageResponse<>(content != null ? content : List.of(), page, totalElements);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PageResponse<PublicCourseResponse> getPublicCourses(
            int page,
            int size,
            ContentLanguage language) {
        PageResponse<PublicCourseResponse> response = getPublicCourses(page, size);
        if (language == null || !language.requiresTranslation()) {
            return response;
        }

        for (PublicCourseResponse post : response.getContent()) {
            post.setTitle(contentTranslationService.translate(
                    "community_post",
                    String.valueOf(post.getPostId()),
                    "title",
                    post.getTitle(),
                    language));
        }
        return response;
    }

    /**
     * 커뮤니티에 공개된 코스 게시글 상세 정보와 연결된 코스 장소 목록 및 댓글 목록을 조회한다.
     */
    public PublicCourseDetailResponse getPublicCourse(Long postId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        PublicCourseDetailPostRow post = postMapper.findPublicCourseDetailById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        List<CoursePlaceResponse> placeRows = courseMapper.findPlacesByCourseId(post.getCourseId());
        List<PublicCourseDetailResponse.PlaceInfo> places = (placeRows == null)
                ? List.of()
                : placeRows.stream()
                        .map(p -> PublicCourseDetailResponse.PlaceInfo.builder()
                                .placeId(p.getPlaceId())
                                .order(p.getVisitOrder())
                                .build())
                        .toList();

        PublicCourseDetailResponse.CourseInfo courseInfo = PublicCourseDetailResponse.CourseInfo.builder()
                .courseId(post.getCourseId())
                .places(places)
                .build();

        List<com.ditto.community.dto.response.CommentResponse> comments = postCommentMapper.findCommentsByPostId(postId);

        return PublicCourseDetailResponse.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .course(courseInfo)
                .comments(comments != null ? comments : List.of())
                .build();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PublicCourseDetailResponse getPublicCourse(Long postId, ContentLanguage language) {
        PublicCourseDetailResponse response = getPublicCourse(postId);
        if (language == null || !language.requiresTranslation()) {
            return response;
        }

        String sourceKey = String.valueOf(response.getPostId());
        response.setTitle(contentTranslationService.translate(
                "community_post", sourceKey, "title", response.getTitle(), language));
        response.setContent(contentTranslationService.translate(
                "community_post", sourceKey, "content", response.getContent(), language));
        return response;
    }

    @Transactional
    public CreateCoursePostResponse createCoursePost(Long userId, CreateCoursePostRequest request) {
        Course course = courseMapper.findById(request.getCourseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.NOT_COURSE_OWNER);
        }

        PostInsertCommand command = PostInsertCommand.builder()
                .courseId(request.getCourseId())
                .userId(userId)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .build();
        postMapper.insert(command);

        return CreateCoursePostResponse.builder()
                .postId(command.getPostId())
                .courseId(command.getCourseId())
                .title(command.getTitle())
                .build();
    }

    @Transactional
    public UpdateCoursePostResponse updateCoursePost(Long userId, Long postId, UpdateCoursePostRequest request) {
        PostRow post = postMapper.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.isWrittenBy(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        String title = request.getTitle().trim();
        String content = request.getContent().trim();
        PostUpdateCommand command = PostUpdateCommand.builder()
                .postId(postId)
                .userId(userId)
                .title(title)
                .content(content)
                .build();

        if (postMapper.update(command) != 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        return UpdateCoursePostResponse.builder()
                .postId(postId)
                .title(title)
                .build();
    }

    @Transactional
    public void deleteCoursePost(Long userId, Long postId) {
        PostRow post = postMapper.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.isWrittenBy(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (postMapper.softDelete(postId, userId) != 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
    }
}
