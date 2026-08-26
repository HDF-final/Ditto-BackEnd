package com.ditto.community.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostImageService {

    /** 한 게시글에 첨부할 수 있는 최대 사진 수. */
    private static final int MAX_IMAGES_PER_POST = 10;

    /** S3 업로드 경로. createObjectKey가 prefix(images/) 아래에 이 디렉터리를 붙인다. */
    private static final String IMAGE_DIRECTORY = "community/posts";

    private final PostMapper postMapper;
    private final PostImageMapper postImageMapper;
    private final S3Provider s3Provider;

    /**
     * 작성자가 자신의 코스 게시글에 사진을 업로드한다.
     * 파일은 S3에 올리고 object key만 DB에 저장한 뒤, 게시글의 전체 사진 URL 목록을 돌려준다.
     */
    @Transactional
    public PostImageUploadResponse uploadImages(Long userId, Long postId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }

        PostRow post = postMapper.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        if (!post.isWrittenBy(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        int existingCount = postImageMapper.countByPostId(postId);
        if (existingCount + files.size() > MAX_IMAGES_PER_POST) {
            throw new BusinessException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        }

        int sortOrder = postImageMapper.nextSortOrder(postId);
        for (MultipartFile file : files) {
            S3UploadResult result = s3Provider.uploadImage(file, IMAGE_DIRECTORY);
            postImageMapper.insert(PostImageInsertCommand.builder()
                    .postId(postId)
                    .imageKey(result.getKey())
                    .sortOrder(sortOrder++)
                    .build());
        }

        List<String> imageUrls = new ArrayList<>();
        for (String key : postImageMapper.findKeysByPostId(postId)) {
            imageUrls.add(s3Provider.resolveDirectImageUrl(key));
        }

        return PostImageUploadResponse.builder()
                .postId(postId)
                .imageUrls(imageUrls)
                .build();
    }
}
