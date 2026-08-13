package com.ditto.security;

/**
 * 세션 {@code SecurityContext} 에 저장되는 인증 주체.
 * 로그인 구현 시 Authentication principal 로 이 객체를 넣는다.
 */
public record AuthUser(Long userId, String email, String role) {
}
