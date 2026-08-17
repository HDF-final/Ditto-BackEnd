package com.ditto.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ditto.global.exception.BusinessException;
import com.ditto.global.exception.ErrorCode;
import com.ditto.news.application.port.out.NewsFeedRepository;
import com.ditto.news.application.service.NewsFeedService;
import com.ditto.news.domain.GeneratedNewsFeed;
import com.ditto.news.domain.NewsFeed;

@SpringBootTest
class NewsFeedCrudComprehensiveLiveTest {

    @Autowired
    private NewsFeedService newsFeedService;

    @Autowired
    private NewsFeedRepository newsFeedRepository;

    @Test
    @EnabledIfEnvironmentVariable(named = "SMOKE_TEST", matches = "true")
    @DisplayName("[R/U/D 종합 검증] 실제 Oracle DB 연동 하에서 모든 정상 및 예외/경계 케이스 테스트")
    @Transactional
    void testAllCrudScenarios() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" >>> 뉴스피드 R / U / D 전체 시나리오 및 예외 케이스 실시간 테스트 시작 <<<");
        System.out.println("=".repeat(80));

        // 1. 사전 데이터 생성 (C)
        String uniqueSlug = "test-slug-" + System.currentTimeMillis();
        GeneratedNewsFeed newFeed = GeneratedNewsFeed.builder()
                .title("테스트 뉴스 제목 🌟")
                .slug(uniqueSlug)
                .representativeImageUrl("https://img.test.com/photo.jpg")
                .body("매우 긴 본문 내용입니다... 줄바꿈\n\n두번째 줄\n'따옴표' 테스트 \"큰따옴표\" 테스트")
                .summaries(List.of("요약 1: 이모지 🇰🇷", "요약 2: 특수문자 & < >", "요약 3: 완료"))
                .keywords(List.of("#KPOP", "#TEST", "#DITTO"))
                .build();

        NewsFeed saved = newsFeedRepository.save(newFeed);
        assertThat(saved).isNotNull();
        System.out.println("✓ [C] 데이터 생성 완료: slug=" + uniqueSlug);

        // 2. Read (R) 테스트 - Slug 단건 조회
        NewsFeed bySlug = newsFeedService.getNewsFeedBySlug(uniqueSlug);
        assertThat(bySlug).isNotNull();
        assertThat(bySlug.getTitle()).isEqualTo("테스트 뉴스 제목 🌟");
        assertThat(bySlug.getSummaries()).hasSize(3);
        assertThat(bySlug.getKeywords()).containsExactly("#KPOP", "#TEST", "#DITTO");
        Long generatedId = bySlug.getNewsFeedId();
        System.out.println("✓ [R] Slug 단건 조회 성공: ID=" + generatedId + ", Title=" + bySlug.getTitle());

        // 3. Read (R) 테스트 - PK ID 단건 조회
        NewsFeed byId = newsFeedService.getNewsFeedById(generatedId);
        assertThat(byId.getSlug()).isEqualTo(uniqueSlug);
        assertThat(byId.getBody()).contains("줄바꿈\n\n두번째 줄");
        System.out.println("✓ [R] PK ID 단건 조회 성공: ID=" + generatedId);

        // 4. Read (R) 테스트 - 페이징 목록 조회
        List<NewsFeed> listPage0 = newsFeedService.getNewsFeeds(0, 10);
        assertThat(listPage0).isNotEmpty();
        System.out.println("✓ [R] 페이징 목록 조회 성공: size=" + listPage0.size());

        // 4-1. Read (R) 테스트 - 사이트맵 목록 조회 (GET /api/v1/news/sitemap)
        List<NewsFeed> sitemapFeeds = newsFeedService.getNewsFeedsForSitemap();
        assertThat(sitemapFeeds).isNotEmpty();
        assertThat(sitemapFeeds.stream().anyMatch(f -> uniqueSlug.equals(f.getSlug()))).isTrue();
        System.out.println("✓ [R] 사이트맵용 경량 목록 조회 성공: sitemap size=" + sitemapFeeds.size());

        // 5. Read (R) 예외 케이스 - 존재하지 않는 ID (404)
        assertThatThrownBy(() -> newsFeedService.getNewsFeedById(999999999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NEWS_FEED_NOT_FOUND);
        System.out.println("✓ [R 예외] 존재하지 않는 ID(999999999L) 조회 시 NEWS_FEED_NOT_FOUND 정상 발생");

        // 6. Read (R) 예외 케이스 - 존재하지 않는 Slug (404)
        assertThatThrownBy(() -> newsFeedService.getNewsFeedBySlug("non-existing-slug-xyz"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NEWS_FEED_NOT_FOUND);
        System.out.println("✓ [R 예외] 존재하지 않는 Slug 조회 시 NEWS_FEED_NOT_FOUND 정상 발생");

        // 7. Read (R) 경계값 케이스 - 음수 페이지, 0 크기 (안전 보정)
        List<NewsFeed> safeList = newsFeedService.getNewsFeeds(-5, 0);
        assertThat(safeList).isNotNull();
        System.out.println("✓ [R 경계값] 음수 페이지(page=-5, size=0) 요청 시 에러 없이 safePage/safeSize로 보정 처리됨");

        // 8. Read (R) 경계값 케이스 - 범위 초과 큰 페이지 (page=99999)
        List<NewsFeed> emptyList = newsFeedService.getNewsFeeds(99999, 10);
        assertThat(emptyList).isEmpty();
        System.out.println("✓ [R 경계값] 범위 초과 페이지(page=99999) 요청 시 빈 리스트 [] 안전 반환");

        // 9. Update (U) 테스트 - 정상 수정
        NewsFeed updated = newsFeedService.updateNewsFeed(
                generatedId,
                "수정된 뉴스 제목 [UPDATE]",
                "수정된 본문 내용...",
                "https://img.test.com/new_photo.jpg",
                List.of("새로운 1줄 요약"),
                List.of("#UPDATE", "#SUCCESS")
        );
        assertThat(updated.getTitle()).isEqualTo("수정된 뉴스 제목 [UPDATE]");
        assertThat(updated.getSummaries()).containsExactly("새로운 1줄 요약");

        NewsFeed reloaded = newsFeedService.getNewsFeedById(generatedId);
        assertThat(reloaded.getTitle()).isEqualTo("수정된 뉴스 제목 [UPDATE]");
        assertThat(reloaded.getRepresentativeImageUrl()).isEqualTo("https://img.test.com/new_photo.jpg");
        System.out.println("✓ [U] 정상 수정 및 재조회 검증 성공: New Title=" + reloaded.getTitle());

        // 10. Update (U) 예외 케이스 - 존재하지 않는 ID 수정 시도 (404)
        assertThatThrownBy(() -> newsFeedService.updateNewsFeed(
                999999999L, "제목", "본문", null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NEWS_FEED_NOT_FOUND);
        System.out.println("✓ [U 예외] 존재하지 않는 ID 수정 시도 시 NEWS_FEED_NOT_FOUND 정상 발생");

        // 11. Delete (D) 테스트 - 정상 삭제
        newsFeedService.deleteNewsFeed(generatedId);
        System.out.println("✓ [D] 정상 삭제 수행 완료: ID=" + generatedId);

        // 12. Delete (D) 후 재조회 검증 (404)
        assertThatThrownBy(() -> newsFeedService.getNewsFeedById(generatedId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NEWS_FEED_NOT_FOUND);
        System.out.println("✓ [D 검증] 삭제 후 재조회 시 NEWS_FEED_NOT_FOUND 정상 발생 확인");

        // 13. Delete (D) 예외 케이스 - 이미 삭제된 ID 재삭제 시도 (404)
        assertThatThrownBy(() -> newsFeedService.deleteNewsFeed(generatedId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NEWS_FEED_NOT_FOUND);
        System.out.println("✓ [D 예외] 이미 삭제된 ID 재삭제 시도 시 NEWS_FEED_NOT_FOUND 정상 발생 확인");

        System.out.println("\n" + "=".repeat(80));
        System.out.println(" >>> 모든 R / U / D 정상 및 예외 케이스 테스트 100% 통과 <<<");
        System.out.println("=".repeat(80) + "\n");
    }
}
