package com.ditto.user.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRow {

    private Long userId;
    private String name;
    private String email;
    private String passwordHash;
    private String preferredLanguageCode;
    private String status;
    private String role;
    private String persona;
}
