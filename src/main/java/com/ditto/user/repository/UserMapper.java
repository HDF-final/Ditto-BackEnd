package com.ditto.user.repository;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    int countByEmail(String email);

    int insert(SignupUserCommand command);
}
