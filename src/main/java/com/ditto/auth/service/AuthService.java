package com.ditto.auth.service;

import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.auth.dto.request.SignupRequest;
import com.ditto.auth.dto.response.SignupResponse;
import com.ditto.country.repository.CountryMapper;
import com.ditto.country.repository.CountryMapper.CountryRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.user.domain.UserRole;
import com.ditto.user.domain.UserStatus;
import com.ditto.user.repository.UserMapper;
import com.ditto.user.repository.UserMapper.SignupUserCommand;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[a-z0-9]{4,10}$");
    private static final String DEFAULT_COUNTRY_CODE = "KR";

    private final UserMapper userMapper;
    private final CountryMapper countryMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validateEmail(request.userEmail());
        validateNickname(request.nickname());
        validateDuplicate(request.userEmail(), request.nickname());

        CountryRow country = countryMapper.findActiveByCode(DEFAULT_COUNTRY_CODE)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEFAULT_COUNTRY_NOT_FOUND));
        UserRole role = resolveSignupRole(request.role());

        SignupUserCommand command = new SignupUserCommand(
                country.countryId(),
                request.name(),
                request.userEmail(),
                request.nickname(),
                request.phone(),
                passwordEncoder.encode(request.password()),
                request.address().bcode(),
                request.address().jibunAddress(),
                request.address().roadAddress(),
                request.address().detail(),
                role.name(),
                country.defaultLanguageCode(),
                UserStatus.ACTIVE.name());

        userMapper.insert(command);
        return new SignupResponse(request.userEmail(), role.name());
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
        }
    }

    private void validateNickname(String nickname) {
        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new BusinessException(ErrorCode.INVALID_NICKNAME_FORMAT);
        }
    }

    private void validateDuplicate(String email, String nickname) {
        if (userMapper.countByEmail(email) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_SIGNUP_EMAIL);
        }
        if (userMapper.countByNickname(nickname) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private UserRole resolveSignupRole(String role) {
        try {
            return UserRole.fromSignupRole(role);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

}
