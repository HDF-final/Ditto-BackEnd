package com.ditto.course.repository;

import org.apache.ibatis.annotations.Mapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Mapper
public interface PostMapper {

    int insert(PostInsertCommand command);

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class PostInsertCommand {
        private Long postId;
        private Long courseId;
        private Long userId;
        private String title;
        private String content;
    }
}
