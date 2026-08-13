package com.ditto.security;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 세션 {@code SecurityContext} 에 저장되는 인증 주체.
 */
@Getter
@AllArgsConstructor
public class AuthUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String email;
    private final String role;
}
