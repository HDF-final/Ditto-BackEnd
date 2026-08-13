package com.ditto.user.repository;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    int countByEmail(String email);

    Optional<UserRow> findActiveByEmail(String email);

    Optional<UserRow> findActiveById(Long userId);

    int insert(SignupUserCommand command);
}
