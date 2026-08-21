package com.ditto.global.infrastructure.translation.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TranslationCacheMapper {

    TranslationCacheEntry find(
            @Param("sourceType") String sourceType,
            @Param("sourceKey") String sourceKey,
            @Param("sourceField") String sourceField,
            @Param("targetLanguage") String targetLanguage);

    int ensureCacheRow(
            @Param("sourceType") String sourceType,
            @Param("sourceKey") String sourceKey,
            @Param("sourceField") String sourceField,
            @Param("targetLanguage") String targetLanguage,
            @Param("sourceHash") String sourceHash);

    int claim(
            @Param("sourceType") String sourceType,
            @Param("sourceKey") String sourceKey,
            @Param("sourceField") String sourceField,
            @Param("targetLanguage") String targetLanguage,
            @Param("sourceHash") String sourceHash,
            @Param("leaseSeconds") long leaseSeconds);

    int markSuccess(
            @Param("sourceType") String sourceType,
            @Param("sourceKey") String sourceKey,
            @Param("sourceField") String sourceField,
            @Param("targetLanguage") String targetLanguage,
            @Param("sourceHash") String sourceHash,
            @Param("translatedText") String translatedText);

    int markFailure(
            @Param("sourceType") String sourceType,
            @Param("sourceKey") String sourceKey,
            @Param("sourceField") String sourceField,
            @Param("targetLanguage") String targetLanguage,
            @Param("sourceHash") String sourceHash,
            @Param("retryAfter") java.time.LocalDateTime retryAfter,
            @Param("lastError") String lastError);

}
