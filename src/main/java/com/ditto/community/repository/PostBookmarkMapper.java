package com.ditto.community.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ditto.user.dto.response.UserBookmarkResponse;

@Mapper
public interface PostBookmarkMapper {

    int insertBookmark(@Param("postId") Long postId, @Param("userId") Long userId);

    int deleteBookmark(@Param("postId") Long postId, @Param("userId") Long userId);

    boolean existsByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    int countByPostId(@Param("postId") Long postId);

    List<UserBookmarkResponse> findBookmarksByUserId(
            @Param("userId") Long userId,
            @Param("offset") long offset,
            @Param("size") int size);

    long countBookmarksByUserId(@Param("userId") Long userId);
}
