package com.ditto.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.country.domain.SupportedCountry;
import com.ditto.country.repository.CountryMapper;
import com.ditto.country.repository.CountryRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.user.domain.Persona;
import com.ditto.user.domain.PreferredLanguage;
import com.ditto.user.dto.request.UpdatePersonaRequest;
import com.ditto.user.dto.request.UpdateUserPreferencesRequest;
import com.ditto.user.dto.request.UpdateUserProfileRequest;
import com.ditto.user.dto.response.PersonaResponse;
import com.ditto.user.dto.response.UserPreferencesResponse;
import com.ditto.user.dto.response.UserProfileResponse;
import com.ditto.user.repository.UpdateUserPreferencesCommand;
import com.ditto.user.repository.UpdateUserCommand;
import com.ditto.user.repository.UserMapper;
import com.ditto.user.repository.UserRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserMapper userMapper;
    private final CountryMapper countryMapper;
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
     * 콘텐츠 대상 국가와 표시 언어를 서로 독립적으로 업데이트한다.
     */
    @Transactional
    public UserPreferencesResponse updatePreferences(Long userId, UpdateUserPreferencesRequest request) {
        userMapper.findActiveById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        SupportedCountry supportedCountry = SupportedCountry.fromCode(request.getCountryCode());
        if (supportedCountry == null) {
            throw new BusinessException(ErrorCode.INVALID_COUNTRY_CODE);
        }
        String countryCode = supportedCountry.name();
        CountryRow country = countryMapper.findActiveByCode(countryCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_COUNTRY_CODE));

        PreferredLanguage language = PreferredLanguage.fromCode(request.getLanguageCode());
        if (language == null) {
            throw new BusinessException(ErrorCode.INVALID_LANGUAGE_CODE);
        }

        UpdateUserPreferencesCommand command = UpdateUserPreferencesCommand.builder()
                .userId(userId)
                .countryId(country.getCountryId())
                .languageCode(language.getCode())
                .build();

        if (userMapper.updatePreferences(command) != 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return UserPreferencesResponse.builder()
                .countryCode(countryCode)
                .languageCode(language.getCode())
                .build();
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
