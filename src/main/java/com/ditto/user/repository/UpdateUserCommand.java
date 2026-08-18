package com.ditto.user.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserCommand {

    private Long userId;
    private String name;
    private String passwordHash;
    private String persona;
}
