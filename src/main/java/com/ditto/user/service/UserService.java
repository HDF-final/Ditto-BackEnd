package com.ditto.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.user.domain.Persona;
import com.ditto.user.dto.request.UpdatePersonaRequest;
import com.ditto.user.dto.request.UpdateUserProfileRequest;
import com.ditto.user.dto.response.PersonaResponse;
import com.ditto.user.dto.response.UserProfileResponse;
import com.ditto.user.repository.UpdateUserCommand;
import com.ditto.user.repository.UserMapper;
import com.ditto.user.repository.UserRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회한다.
     */
    public UserProfileResponse getMyProfile(Long userId) {
        UserRow user = userMapper.findActiveById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserProfileResponse.from(user);
    }

    /**
     * 사용자의 닉네임, 비밀번호, 페르소나 정보를 수정한다.
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        UserRow user = userMapper.findActiveById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String newPasswordHash = null;
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            newPasswordHash = passwordEncoder.encode(request.getPassword().trim());
        }

        String personaCode = null;
        if (request.getPersona() != null && !request.getPersona().isBlank()) {
            Persona persona = Persona.from(request.getPersona());
            if (persona == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            personaCode = persona.name();
        }

        String newName = null;
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            newName = request.getNickname().trim();
        }

        UpdateUserCommand command = UpdateUserCommand.builder()
                .userId(userId)
                .name(newName)
                .passwordHash(newPasswordHash)
                .persona(personaCode)
                .build();

        userMapper.updateProfile(command);

        UserRow updatedUser = userMapper.findActiveById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserProfileResponse.from(updatedUser);
    }

    /**
     * 사용자의 쇼핑 페르소나를 조회한다.
     */
    public PersonaResponse getPersona(Long userId) {
        UserRow user = userMapper.findActiveById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return PersonaResponse.from(user.getPersona());
    }

    /**
     * 사용자의 쇼핑 페르소나를 업데이트한다. (온보딩 및 마이페이지)
     */
    @Transactional
    public PersonaResponse updatePersona(Long userId, UpdatePersonaRequest request) {
        UserRow user = userMapper.findActiveById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Persona persona = Persona.from(request.getPersona());
        if (persona == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        userMapper.updatePersona(userId, persona.name());

        return PersonaResponse.from(persona.name());
    }
}
