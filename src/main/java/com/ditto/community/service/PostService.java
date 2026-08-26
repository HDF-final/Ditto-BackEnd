package com.ditto.community.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.ditto.community.repository.PostImageMapper;
import com.ditto.community.repository.PostImageMapper.PostImageKeyRow;
import com.ditto.global.i18n.ContentLanguage;
import com.ditto.global.infrastructure.s3.S3Provider;
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
    private final PostImageMapper postImageMapper;
    private final S3Provider s3Provider;
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

        if (content == null || content.isEmpty()) {
            return new PageResponse<>(List.of(), page, totalElements);
        }

        attachImageUrls(content);
        return new PageResponse<>(content, page, totalElements);
    }

    /**
     * 목록의 각 게시글에 첨부 사진 URL을 채운다.
     * 게시글 수만큼 쿼리하지 않도록 사진 key를 한 번에 조회한 뒤 게시글별로 묶어 URL로 변환한다.
     */
    private void attachImageUrls(List<PublicCourseResponse> content) {
        List<Long> postIds = content.stream()
                .map(PublicCourseResponse::getPostId)
                .toList();

        Map<Long, List<String>> urlsByPostId = new LinkedHashMap<>();
        for (PostImageKeyRow row : postImageMapper.findKeysByPostIds(postIds)) {
            // 저장값은 S3 object key다. 버킷이 공개 읽기라 셀럽 사진(course/*)과 같은 버킷 직통 URL로 내려준다.
            // images/* 는 CloudFront behavior가 없어 resolveImageUrl로는 프로덕션에서 301로 튕긴다.
            String url = s3Provider.resolveDirectImageUrl(row.getImageKey());
            urlsByPostId.computeIfAbsent(row.getPostId(), key -> new ArrayList<>()).add(url);
        }

        for (PublicCourseResponse post : content) {
            post.setImageUrls(urlsByPostId.getOrDefault(post.getPostId(), List.of()));
        }
    }

    /** 게시글 하나의 첨부 사진 key를 정렬 순서대로 조회해 버킷 직통 공개 URL로 변환한다. */
    private List<String> resolveImageUrls(Long postId) {
        List<String> urls = new ArrayList<>();
        for (String key : postImageMapper.findKeysByPostId(postId)) {
            urls.add(s3Provider.resolveDirectImageUrl(key));
        }
        return urls;
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
                .imageUrls(resolveImageUrls(post.getPostId()))
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
