package com.ditto.news.outbound.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.ditto.news.application.port.out.NewsFeedRepository;
import com.ditto.news.domain.NewsFeed;
import com.ditto.news.outbound.repository.entity.NewsFeedRow;
import com.ditto.news.outbound.repository.mapper.NewsFeedMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link NewsFeedRepository} 인터페이스 구현체.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class NewsFeedRepositoryImpl implements NewsFeedRepository {

    private final NewsFeedMapper newsFeedMapper;
    private final ObjectMapper objectMapper;

    @Override
    public NewsFeed save(NewsFeed newsFeed) {
        if (newsFeed == null) {
            return null;
        }

        String summaryJson = serializeList(newsFeed.getSummaries());
        String keywordsJson = serializeList(newsFeed.getKeywords());

        newsFeedMapper.insert(
                newsFeed.getTitle(),
                newsFeed.getSlug(),
                newsFeed.getRepresentativeImageUrl(),
                newsFeed.getBody(),
                summaryJson,
                keywordsJson
        );

        log.info("뉴스피드 DB 저장 완료: title='{}', slug='{}'", newsFeed.getTitle(), newsFeed.getSlug());
        return newsFeed;
    }

    @Override
    public Optional<NewsFeed> findById(Long newsFeedId) {
        if (newsFeedId == null) {
            return Optional.empty();
        }
        return newsFeedMapper.findById(newsFeedId).map(this::toDomain);
    }

    @Override
    public Optional<NewsFeed> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return newsFeedMapper.findBySlug(slug).map(this::toDomain);
    }

    @Override
    public List<NewsFeed> findAll(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : Math.min(size, 50);
        int offset = safePage * safeSize;

        List<NewsFeedRow> rows = newsFeedMapper.findAll(offset, safeSize);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(this::toDomain).toList();
    }

    @Override
    public List<NewsFeed> findAllForSitemap() {
        List<NewsFeedRow> rows = newsFeedMapper.findAllForSitemap();
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(this::toDomain).toList();
    }

    @Override
    public long count() {
        return newsFeedMapper.count();
    }

    @Override
    public void update(NewsFeed newsFeed) {
        if (newsFeed == null || newsFeed.getNewsFeedId() == null) {
            return;
        }
        String summaryJson = serializeList(newsFeed.getSummaries());
        String keywordsJson = serializeList(newsFeed.getKeywords());

        newsFeedMapper.update(
                newsFeed.getNewsFeedId(),
                newsFeed.getTitle(),
                newsFeed.getBody(),
                newsFeed.getRepresentativeImageUrl(),
                summaryJson,
                keywordsJson
        );
        log.info("뉴스피드 DB 수정 완료: newsFeedId={}", newsFeed.getNewsFeedId());
    }

    @Override
    public void deleteById(Long newsFeedId) {
        if (newsFeedId == null) {
            return;
        }
        newsFeedMapper.deleteById(newsFeedId);
        log.info("뉴스피드 DB 삭제 완료: newsFeedId={}", newsFeedId);
    }

    private NewsFeed toDomain(NewsFeedRow row) {
        return NewsFeed.builder()
                .newsFeedId(row.getNewsFeedId())
                .title(row.getTitle())
                .slug(row.getSlug())
                .representativeImageUrl(row.getRepresentativeImageUrl())
                .body(row.getBody())
                .summaries(deserializeList(row.getSummary()))
                .keywords(deserializeList(row.getKeywords()))
                .createdAt(row.getCreatedAt())
                .deletedAt(row.getDeletedAt())
                .build();
    }

    private String serializeList(Object list) {
        if (list == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("리스트 JSON 직렬화 실패: {}", e.getMessage());
            return list.toString();
        }
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.debug("JSON 파싱 실패, 단일 문자열로 취급: {}", json);
            return List.of(json);
        }
    }
}
