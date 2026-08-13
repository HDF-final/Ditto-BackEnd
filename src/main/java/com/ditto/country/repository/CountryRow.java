package com.ditto.country.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryRow {

    private Long countryId;
    private String defaultLanguageCode;
}
