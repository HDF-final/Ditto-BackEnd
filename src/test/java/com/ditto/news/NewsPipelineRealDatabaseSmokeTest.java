package com.ditto.news;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ditto.news.application.service.NewsFeedPipelineService;
import com.ditto.news.inbound.rest.dto.response.NewsPipelineDebugResponse;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SMOKE_TEST", matches = "true",
        disabledReason = "수동 실행 전용 Smoke Test입니다. 실행하려면 SMOKE_TEST=true 환경변수를 전달하세요.")
class NewsPipelineRealDatabaseSmokeTest {

    @Autowired
    private NewsFeedPipelineService pipelineService;

    @Test
    @DisplayName("실제 RSS -> Python 크롤러 -> Gemini AI -> Oracle DB INSERT 5단계 전체 파이프라인 실시간 실행")
    void runFullPipelineAndInsertToOracle() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" >>> DITTO AI 뉴스 파이프라인 5단계 전체 실시간 실행 (DB INSERT 연동) <<<");
        System.out.println("=".repeat(80));

        NewsPipelineDebugResponse response = pipelineService.executePipelineWithDebug("K-POP");

        if (response != null && response.getGeneratedFeed() != null) {
            System.out.println("\n🎉 [5단계 파이프라인 실행 성공]");
            System.out.println(" - 1단계 수집 후보 수: " + response.getCandidateCount() + "건");
            System.out.println(" - 2단계 크롤링 성공 수: " + response.getCrawledCount() + "건");
            System.out.println(" - 3단계 선별 기사 수: " + response.getSelectedCount() + "건");
            System.out.println(" - 4단계 AI 생성 제목: " + response.getGeneratedFeed().getTitle());
            System.out.println(" - 4단계 3줄 요약:");
            if (response.getGeneratedFeed().getSummaries() != null) {
                response.getGeneratedFeed().getSummaries().forEach(s -> System.out.println("    • " + s));
            }
            System.out.println(" - 5단계 DB INSERT PK ID: " + response.getSavedNewsFeedId());
        } else {
            System.out.println(" -> 파이프라인 실행 결과가 없습니다.");
        }

        System.out.println("\n" + "=".repeat(80) + "\n");
    }
}
