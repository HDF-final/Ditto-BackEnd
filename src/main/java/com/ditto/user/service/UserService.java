package com.ditto.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.user.domain.Persona;
import com.ditto.user.dto.request.UpdatePersonaRequest;
import com.ditto.user.dto.response.PersonaResponse;
import com.ditto.user.repository.UserMapper;
import com.ditto.user.repository.UserRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserMapper userMapper;

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
