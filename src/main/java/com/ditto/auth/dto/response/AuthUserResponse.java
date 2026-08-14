package com.ditto.auth.dto.response;

import com.ditto.user.repository.UserRow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthUserResponse {

    private final Long userId;
    private final String name;
    private final String email;
    private final String preferredLanguageCode;

    public static AuthUserResponse from(UserRow user) {
        return AuthUserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .preferredLanguageCode(user.getPreferredLanguageCode())
                .build();
    }
}
