package com.ditto.user.repository;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    int countByEmail(String email);

    Optional<UserRow> findActiveByEmail(String email);

    Optional<UserRow> findActiveById(Long userId);

    int insert(SignupUserCommand command);

    int updatePersona(@Param("userId") Long userId, @Param("persona") String persona);

    int updateProfile(UpdateUserCommand command);

    int updatePreferences(UpdateUserPreferencesCommand command);
}
