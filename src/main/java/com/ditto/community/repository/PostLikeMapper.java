package com.ditto.community.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ditto.user.dto.response.UserLikeResponse;

@Mapper
public interface PostLikeMapper {

    int insertLike(@Param("postId") Long postId, @Param("userId") Long userId);

    int deleteLike(@Param("postId") Long postId, @Param("userId") Long userId);

    boolean existsByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    int countByPostId(@Param("postId") Long postId);

    List<UserLikeResponse> findLikesByUserId(
            @Param("userId") Long userId,
            @Param("offset") long offset,
            @Param("size") int size);

    long countLikesByUserId(@Param("userId") Long userId);
}
