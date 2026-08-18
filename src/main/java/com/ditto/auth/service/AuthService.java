package com.ditto.auth.service;

import java.util.regex.Pattern;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ditto.auth.dto.request.LoginRequest;
import com.ditto.auth.dto.request.SignupRequest;
import com.ditto.auth.dto.response.AuthUserResponse;
import com.ditto.auth.dto.response.LoginResponse;
import com.ditto.auth.dto.response.SignupResponse;
import com.ditto.country.repository.CountryMapper;
import com.ditto.country.repository.CountryRow;
import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.security.AuthUser;
import com.ditto.user.domain.UserStatus;
import com.ditto.user.domain.UserRole;
import com.ditto.user.repository.UserMapper;
import com.ditto.user.repository.SignupUserCommand;
import com.ditto.user.repository.UserRow;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserMapper userMapper;
    private final CountryMapper countryMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validateEmail(request.getUserEmail());
        validateDuplicateEmail(request.getUserEmail());

        String countryCode = request.getCountryCode() != null ? request.getCountryCode().trim().toUpperCase() : "";
        CountryRow country = countryMapper.findActiveByCode(countryCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEFAULT_COUNTRY_NOT_FOUND));

        String personaCode = null;
        if (request.getPersona() != null && !request.getPersona().isBlank()) {
            com.ditto.user.domain.Persona persona = com.ditto.user.domain.Persona.from(request.getPersona());
            personaCode = (persona != null) ? persona.name() : request.getPersona().trim();
        }

        SignupUserCommand command = SignupUserCommand.builder()
                .countryId(country.getCountryId())
                .name(request.getName())
                .email(request.getUserEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .preferredLanguageCode(country.getDefaultLanguageCode())
                .status(UserStatus.ACTIVE.name())
                .role(UserRole.ROLE_CUSTOMER.name())
                .persona(personaCode)
                .build();

        userMapper.insert(command);
        return SignupResponse.builder()
                .userEmail(request.getUserEmail())
                .build();
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        validateEmail(request.getUserEmail());
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_PASSWORD_REQUIRED);
        }

        UserRow user = userMapper.findActiveByEmail(request.getUserEmail())
                .filter(activeUser -> passwordEncoder.matches(request.getPassword(), activeUser.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_UNAUTHENTICATED));

        String role = user.getRole();
        AuthUser principal = new AuthUser(user.getUserId(), user.getEmail(), role);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(role)));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        httpRequest.getSession(true);
        httpRequest.changeSessionId();
        securityContextRepository.saveContext(context, httpRequest, response);

        return LoginResponse.from(user, role.replace("ROLE_", ""));
    }

    public AuthUserResponse getMe(Long userId) {
        UserRow user = userMapper.findActiveById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return AuthUserResponse.from(user);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
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
