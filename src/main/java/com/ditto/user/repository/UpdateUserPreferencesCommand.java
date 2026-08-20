package com.ditto.user.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserPreferencesCommand {

    private Long userId;
    private Long countryId;
    private String languageCode;
}
