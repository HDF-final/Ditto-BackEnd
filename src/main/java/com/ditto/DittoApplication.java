package com.ditto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;
import com.ditto.config.EnvFileLoader;
import jakarta.annotation.PostConstruct;

@SpringBootApplication(excludeName = {
        // RAG 빈을 붙이기 전까지 pgvector / 이중 임베딩 자동설정을 끈다.
        // bedrock 스타터가 Titan + Cohere 를 동시에 띄우면 VectorStore 기동이 실패한다.
        "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
        "org.springframework.ai.model.bedrock.cohere.autoconfigure.BedrockCohereEmbeddingAutoConfiguration",
        "org.springframework.ai.model.bedrock.titan.autoconfigure.BedrockTitanEmbeddingAutoConfiguration"
})
public class DittoApplication {

    @PostConstruct
    void started() {
        // 서비스 전역 기준 시간대: Asia/Seoul
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        EnvFileLoader.load();
        SpringApplication.run(DittoApplication.class, args);
    }
}
