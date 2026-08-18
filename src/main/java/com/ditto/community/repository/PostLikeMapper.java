package com.ditto.community.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostLikeMapper {

    int insertLike(@Param("postId") Long postId, @Param("userId") Long userId);

    int deleteLike(@Param("postId") Long postId, @Param("userId") Long userId);

    boolean existsByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    int countByPostId(@Param("postId") Long postId);
}
