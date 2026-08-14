package com.ditto.user.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupUserCommand {

    private Long countryId;
    private String name;
    private String email;
    private String passwordHash;
    private String preferredLanguageCode;
    private String status;
    private String role;
}
