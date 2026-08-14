package com.ditto.auth.dto.response;

import com.ditto.user.repository.UserRow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private final boolean authenticated;
    private final String email;
    private final String nickname;
    private final String role;

    public static LoginResponse from(UserRow user, String role) {
        return LoginResponse.builder()
                .authenticated(true)
                .email(user.getEmail())
                .nickname(user.getName())
                .role(role)
                .build();
    }
}
