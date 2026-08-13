package com.ditto.country.repository;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CountryMapper {

    Optional<CountryRow> findActiveByCode(String code);
}
