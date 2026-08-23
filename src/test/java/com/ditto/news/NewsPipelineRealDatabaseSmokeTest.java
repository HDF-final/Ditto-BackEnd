package com.ditto.news;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ditto.news.application.service.NewsFeedPipelineService;
import com.ditto.news.inbound.rest.dto.response.NewsPipelineDebugResponse;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "SMOKE_TEST", matches = "true",
        disabledReason = "수동 실행 전용 Smoke Test입니다. 실행하려면 SMOKE_TEST=true 환경변수를 전달하세요.")
class NewsPipelineRealDatabaseSmokeTest {

    @org.junit.jupiter.api.BeforeAll
    static void init() {
        com.ditto.config.EnvFileLoader.load();
    }

    @Autowired
    private NewsFeedPipelineService pipelineService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Oracle DB NEWS_FEED 테이블 전체 행 조회")
    void checkRowsInDb() {
        var rows = jdbcTemplate.queryForList("SELECT news_feed_id, title, slug, representative_image_url, created_at FROM news_feed ORDER BY news_feed_id DESC");
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" >>> Oracle RDS NEWS_FEED 현재 전체 레코드 (" + rows.size() + "건) <<<");
        System.out.println("=".repeat(80));
        for (var row : rows) {
            System.out.println("ID: " + row.get("NEWS_FEED_ID") + " | 제목: " + row.get("TITLE") + " | 이미지: " + row.get("REPRESENTATIVE_IMAGE_URL") + " | 생성: " + row.get("CREATED_AT"));
        }
        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @DisplayName("실제 RSS -> Python 크롤러 -> Gemini AI -> Oracle DB INSERT 5단계 전체 파이프라인 실시간 실행")
    void runFullPipelineAndInsertToOracle() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" >>> DITTO AI 뉴스 파이프라인 5단계 전체 실시간 실행 (DB INSERT 연동) <<<");
        System.out.println("=".repeat(80));

        NewsPipelineDebugResponse response = pipelineService.executePipelineWithDebug("K-POP");

        if (response != null && response.getGeneratedFeeds() != null && !response.getGeneratedFeeds().isEmpty()) {
            System.out.println("\n🎉 [5단계 파이프라인 실행 성공 - 총 " + response.getGeneratedFeeds().size() + "개 뉴스피드 S3 업로드 & DB 영속화 완료]");
            System.out.println(" - 1단계 수집 후보 수: " + response.getCandidateCount() + "건");
            System.out.println(" - 2단계 크롤링 성공 수: " + response.getCrawledCount() + "건");
            System.out.println(" - 3단계 선별 기사 수: " + response.getSelectedCount() + "건");

            int idx = 1;
            for (com.ditto.news.domain.GeneratedNewsFeed feed : response.getGeneratedFeeds()) {
                System.out.println("\n" + "=".repeat(70));
                System.out.printf(" [뉴스피드 #%d] %s%n", idx++, feed.getTitle());
                System.out.println("=".repeat(70));
                System.out.println("  • 슬러그: " + feed.getSlug());
                System.out.println("  • ⭐️ S3 대표 이미지 URL: " + feed.getRepresentativeImageUrl());
                System.out.println("  • 원본 출처 URL: " + feed.getSourceUrl());
                System.out.println("  • 키워드: " + feed.getKeywords());
                System.out.println("  • 3줄 요약:");
                if (feed.getSummaries() != null) {
                    feed.getSummaries().forEach(s -> System.out.println("     - " + s));
                }
            }
            System.out.println("\n - 5단계 DB INSERT 완료 건수: " + response.getGeneratedFeeds().size() + "건");
        } else {
            System.out.println(" -> 파이프라인 실행 결과가 없습니다.");
        }

        System.out.println("\n" + "=".repeat(80) + "\n");
    }
}
