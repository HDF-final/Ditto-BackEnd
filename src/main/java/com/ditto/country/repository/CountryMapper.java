package com.ditto.country.repository;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CountryMapper {

    @Select("""
            SELECT country_id AS countryId,
                   default_language_code AS defaultLanguageCode
              FROM country
             WHERE code = #{code}
               AND status = 'ACTIVE'
            """)
    Optional<CountryRow> findActiveByCode(String code);

    record CountryRow(
            Long countryId,
            String defaultLanguageCode) {
    }
}
