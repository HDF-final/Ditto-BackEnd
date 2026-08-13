package com.ditto.security;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * local profile 전용 임시 인증 필터.
 * 로그인 API가 붙기 전까지 Postman에서 X-User-Id 헤더만으로 세션 없이 API를 테스트한다.
 */
@Component
@Profile("local")
public class LocalHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateFromHeader(request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateFromHeader(HttpServletRequest request) {
        String userIdValue = request.getHeader(USER_ID_HEADER);
        if (!StringUtils.hasText(userIdValue)) {
            return;
        }

        Long userId = parseUserId(userIdValue);
        if (userId == null) {
            return;
        }
        String email = resolveHeader(request, USER_EMAIL_HEADER, "local-user@example.com");
        String role = normalizeRole(resolveHeader(request, USER_ROLE_HEADER, "ROLE_CUSTOMER"));

        AuthUser principal = new AuthUser(userId, email, role);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Long parseUserId(String userIdValue) {
        try {
            return Long.valueOf(userIdValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeRole(String role) {
        if (role.startsWith("ROLE_")) {
            return role;
        }
        return "ROLE_" + role;
    }

    private String resolveHeader(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getHeader(name);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value;
    }
}
