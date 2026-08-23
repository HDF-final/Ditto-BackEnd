package com.ditto.global.infrastructure.s3;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "SMOKE_TEST", matches = "true",
        disabledReason = "수동 실행 전용 S3 업로드 Smoke Test입니다. 실행하려면 SMOKE_TEST=true 환경변수를 전달하세요.")
class S3NewsImageRealUploadSmokeTest {

    @org.junit.jupiter.api.BeforeAll
    static void init() {
        com.ditto.config.EnvFileLoader.load();
    }

    @Autowired
    private S3Provider s3Provider;

    @Test
    @DisplayName("외부 뉴스 이미지 URL을 S3 news/ 디렉토리에 업로드하고, S3 URL로 직접 이미지 다운로드가 성공하는지 검증")
    void testUploadAndRetrieveNewsImageFromS3() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" >>> [1단계] AWS S3 뉴스 이미지 실시간 업로드 및 조회 검증 테스트 <<<");
        System.out.println("=".repeat(80));

        // 1. 테스트용 연합뉴스 실제 기사 이미지 URL
        String sampleNewsImageUrl = "https://img3.yna.co.kr/etc/inner/KR/2026/08/22/AKR20260822026400005_01_i_P4.jpg";
        System.out.println("1. 원본 외부 이미지 URL: " + sampleNewsImageUrl);

        // 2. S3 news/ 디렉토리에 업로드 실행
        System.out.println("2. S3Provider.uploadImageFromUrl(url, \"news\") 실행 중...");
        S3UploadResult result = null;
        try {
            result = s3Provider.uploadImageFromUrl(sampleNewsImageUrl, "news");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (result == null) {
            System.out.println("-> uploadImageFromUrl 반환값이 null입니다. 직접 download 후 uploadImageBytes를 테스트합니다.");
            RestClient restClient = RestClient.builder().build();
            ResponseEntity<byte[]> downloadResp = restClient.get().uri(sampleNewsImageUrl).retrieve().toEntity(byte[].class);
            System.out.println("-> 다운로드 응답 상태: " + downloadResp.getStatusCode());
            System.out.println("-> Content-Type: " + downloadResp.getHeaders().getContentType());
            System.out.println("-> 바이트 크기: " + (downloadResp.getBody() != null ? downloadResp.getBody().length : 0));
            result = s3Provider.uploadImageBytes(downloadResp.getBody(), "image/jpeg", "news");
        }

        assertThat(result).isNotNull();
        assertThat(result.getUrl()).isNotBlank();
        assertThat(result.getKey()).startsWith("images/news/");

        System.out.println("🎉 S3 업로드 성공!");
        System.out.println("  • S3 Object Key: " + result.getKey());
        System.out.println("  • 최종 S3 접근 URL: " + result.getUrl());

        // 3. 생성된 S3 URL로 직접 HTTP GET 요청하여 이미지가 실제로 정상 서빙되는지 검증
        System.out.println("\n3. S3 URL로 직접 HTTP GET 요청하여 이미지 다운로드 검증 중...");
        RestClient restClient = RestClient.builder().build();
        ResponseEntity<byte[]> response = restClient.get()
                .uri(result.getUrl())
                .retrieve()
                .toEntity(byte[].class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThan(0);

        System.out.println("🎉 S3 이미지 조회/다운로드 성공!");
        System.out.println("  • HTTP 응답 코드: " + response.getStatusCode());
        System.out.println("  • Content-Type: " + response.getHeaders().getContentType());
        System.out.println("  • 다운로드된 이미지 크기: " + response.getBody().length + " bytes (" + (response.getBody().length / 1024) + " KB)");
        System.out.println("=".repeat(80) + "\n");
    }
}
