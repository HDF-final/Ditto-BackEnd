package com.ditto.auth.service;

import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.auth.dto.request.SignupRequest;
import com.ditto.auth.dto.response.SignupResponse;
import com.ditto.country.repository.CountryMapper;
import com.ditto.country.repository.CountryRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.user.domain.UserStatus;
import com.ditto.user.repository.UserMapper;
import com.ditto.user.repository.SignupUserCommand;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserMapper userMapper;
    private final CountryMapper countryMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validateEmail(request.getUserEmail());
        validateDuplicateEmail(request.getUserEmail());

        CountryRow country = countryMapper.findActiveByCode(request.getCountryCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEFAULT_COUNTRY_NOT_FOUND));

        SignupUserCommand command = SignupUserCommand.builder()
                .countryId(country.getCountryId())
                .name(request.getName())
                .email(request.getUserEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .preferredLanguageCode(country.getDefaultLanguageCode())
                .status(UserStatus.ACTIVE.name())
                .build();

        userMapper.insert(command);
        return SignupResponse.builder()
                .userEmail(request.getUserEmail())
                .build();
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
        }
    }

    private void validateDuplicateEmail(String email) {
        if (userMapper.countByEmail(email) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_SIGNUP_EMAIL);
        }
    }

}
