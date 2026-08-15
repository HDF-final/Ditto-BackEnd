package com.ditto.community.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.community.dto.request.CreateCoursePostRequest;
import com.ditto.community.dto.request.UpdateCoursePostRequest;
import com.ditto.community.dto.response.CreateCoursePostResponse;
import com.ditto.community.dto.response.UpdateCoursePostResponse;
import com.ditto.course.domain.Course;
import com.ditto.course.repository.CourseMapper;
import com.ditto.course.repository.PostMapper;
import com.ditto.course.repository.PostMapper.PostInsertCommand;
import com.ditto.course.repository.PostMapper.PostRow;
import com.ditto.course.repository.PostMapper.PostUpdateCommand;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final CourseMapper courseMapper;
    private final PostMapper postMapper;

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
}
