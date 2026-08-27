package com.ditto.community.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ditto.community.dto.request.CreateCoursePostRequest;
import com.ditto.community.dto.request.UpdateCoursePostRequest;
import com.ditto.community.dto.response.CreateCoursePostResponse;
import com.ditto.community.dto.response.PostImageResponse;
import com.ditto.community.dto.response.PublicCourseDetailResponse;
import com.ditto.community.dto.response.PublicCourseResponse;
import com.ditto.community.dto.response.UpdateCoursePostResponse;
import com.ditto.community.repository.PostImageMapper;
import com.ditto.community.repository.PostImageMapper.PostImageInsertCommand;
import com.ditto.community.repository.PostImageMapper.PostImageKeyRow;
import com.ditto.community.repository.PostImageMapper.PostImageRow;
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
import com.ditto.global.infrastructure.s3.S3Provider;
import com.ditto.global.infrastructure.s3.S3UploadResult;
import com.ditto.global.infrastructure.translation.ContentTranslationService;
import com.ditto.global.i18n.ContentLanguage;
import com.ditto.user.repository.UserMapper;
import com.ditto.user.repository.UserRow;

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
    private final UserMapper userMapper;
    private final S3Provider s3Provider;
    private final PostImageMapper postImageMapper;

    /**
     * 커뮤니티에 공개된 코스 게시글 목록을 최신순으로 페이징 조회한다.
     */
    public PageResponse<PublicCourseResponse> getPublicCourses(int page, int size) {
        return getPublicCourses(page, size, null, null);
    }

    public PageResponse<PublicCourseResponse> getPublicCourses(
            int page,
            int size,
            Long authorId,
            String author) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String normalizedAuthor = normalizeAuthor(author);
        long offset = (long) page * size;
        boolean hasAuthorFilter = authorId != null || normalizedAuthor != null;
        List<PublicCourseResponse> content = hasAuthorFilter
                ? postMapper.findPublicCourses(offset, size, authorId, normalizedAuthor)
                : postMapper.findPublicCourses(offset, size);
        long totalElements = hasAuthorFilter
                ? postMapper.countPublicCourses(authorId, normalizedAuthor)
                : postMapper.countPublicCourses();

        List<PublicCourseResponse> posts = content != null ? content : List.of();
        attachImageUrls(posts);

        return new PageResponse<>(posts, page, totalElements);
    }

    private String normalizeAuthor(String author) {
        if (author == null || author.isBlank()) {
            return null;
        }
        return author.trim();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PageResponse<PublicCourseResponse> getPublicCourses(
            int page,
            int size,
            ContentLanguage language) {
        return getPublicCourses(page, size, null, null, language);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PageResponse<PublicCourseResponse> getPublicCourses(
            int page,
            int size,
            Long authorId,
            String author,
            ContentLanguage language) {
        PageResponse<PublicCourseResponse> response = getPublicCourses(page, size, authorId, author);
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
        List<PostImageRow> postImages = postImageMapper.findByPostId(postId);

        return PublicCourseDetailResponse.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .writerId(post.getWriterId())
                .writerNickname(post.getWriterNickname())
                .country(post.getCountry())
                .content(post.getContent())
                .imageUrls(resolveImageUrls(postImages))
                .images(toImageResponses(postImages))
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
        UserRow writer = userMapper.findActiveById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return CreateCoursePostResponse.builder()
                .postId(command.getPostId())
                .courseId(command.getCourseId())
                .writerId(writer.getUserId())
                .writerNickname(writer.getName())
                .country(writer.getCountryCode())
                .title(command.getTitle())
                .build();
    }

    @Transactional
    public UpdateCoursePostResponse updateCoursePost(
            Long userId,
            Long postId,
            UpdateCoursePostRequest request) {
        return updateCoursePost(userId, postId, request, List.of());
    }

    @Transactional
    public UpdateCoursePostResponse updateCoursePost(
            Long userId,
            Long postId,
            UpdateCoursePostRequest request,
            List<MultipartFile> images) {
        PostRow post = postMapper.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.isWrittenBy(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        String title = request.getTitle().trim();
        String content = request.getContent().trim();
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Course course = courseMapper.findById(post.getCourseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        PostUpdateCommand command = PostUpdateCommand.builder()
                .postId(postId)
                .userId(userId)
                .title(title)
                .content(content)
                .build();

        if (courseMapper.updateInfo(post.getCourseId(), title, course.getDescription()) != 1) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (postMapper.update(command) != 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        updateImages(postId, request, images);
        List<PostImageRow> postImages = postImageMapper.findByPostId(postId);

        return UpdateCoursePostResponse.builder()
                .postId(postId)
                .title(title)
                .content(content)
                .imageUrls(resolveImageUrls(postImages))
                .images(toImageResponses(postImages))
                .build();
    }

    private void updateImages(
            Long postId,
            UpdateCoursePostRequest request,
            List<MultipartFile> images) {
        List<PostImageRow> deletedImages = deleteRequestedImages(postId, request);
        for (PostImageRow deletedImage : deletedImages) {
            s3Provider.deleteImage(deletedImage.getImageKey());
        }

        List<MultipartFile> uploadImages = images == null
                ? List.of()
                : images.stream()
                        .filter(image -> image != null && !image.isEmpty())
                        .toList();
        int nextSortOrder = postImageMapper.nextSortOrder(postId);
        for (MultipartFile image : uploadImages) {
            S3UploadResult uploadResult = s3Provider.uploadImage(image, "community/posts");
            postImageMapper.insert(PostImageInsertCommand.builder()
                    .postId(postId)
                    .imageKey(uploadResult.getKey())
                    .sortOrder(nextSortOrder++)
                    .build());
        }
    }

    private List<PostImageRow> deleteRequestedImages(Long postId, UpdateCoursePostRequest request) {
        if (Boolean.TRUE.equals(request.getDeleteAllImages())) {
            List<PostImageRow> images = postImageMapper.findByPostId(postId);
            postImageMapper.deleteByPostId(postId);
            return images;
        }

        List<Long> deleteImageIds = request.getDeleteImageIds();
        if (deleteImageIds == null || deleteImageIds.isEmpty()) {
            return List.of();
        }

        List<Long> normalizedIds = deleteImageIds.stream()
                .filter(imageId -> imageId != null && imageId > 0)
                .distinct()
                .toList();
        if (normalizedIds.size() != deleteImageIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<PostImageRow> images = postImageMapper.findByPostIdAndIds(postId, normalizedIds);
        if (images.size() != normalizedIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "삭제할 사진을 찾을 수 없습니다.");
        }
        postImageMapper.deleteByIds(postId, normalizedIds);
        return images;
    }

    private List<String> resolveImageUrls(List<PostImageRow> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return images.stream()
                .map(PostImageRow::getImageKey)
                .map(s3Provider::resolveImageUrl)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<PostImageResponse> toImageResponses(List<PostImageRow> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return images.stream()
                .map(image -> PostImageResponse.builder()
                        .postImageId(image.getPostImageId())
                        .imageUrl(s3Provider.resolveImageUrl(image.getImageKey()))
                        .sortOrder(image.getSortOrder())
                        .build())
                .toList();
    }

    private void attachImageUrls(List<PublicCourseResponse> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        List<Long> postIds = posts.stream()
                .map(PublicCourseResponse::getPostId)
                .filter(postId -> postId != null && postId > 0)
                .distinct()
                .toList();
        if (postIds.isEmpty()) {
            return;
        }

        List<PostImageKeyRow> imageRows = postImageMapper.findKeysByPostIds(postIds);
        if (imageRows == null || imageRows.isEmpty()) {
            posts.forEach(post -> post.setImageUrls(List.of()));
            return;
        }

        Map<Long, List<String>> imageUrlsByPostId = imageRows.stream()
                .filter(row -> row.getPostId() != null && StringUtils.hasText(row.getImageKey()))
                .collect(Collectors.groupingBy(
                        PostImageKeyRow::getPostId,
                        Collectors.mapping(row -> s3Provider.resolveImageUrl(row.getImageKey()), Collectors.toList())));

        posts.forEach(post -> post.setImageUrls(
                imageUrlsByPostId.getOrDefault(post.getPostId(), List.of())));
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
