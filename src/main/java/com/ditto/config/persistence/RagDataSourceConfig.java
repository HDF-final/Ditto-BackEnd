package com.ditto.config.persistence;

import org.springframework.context.annotation.Configuration;

/**
 * RAG 벡터 저장(PostgreSQL + pgvector) 설정 — 구조 골격.
 *
 * <p>TODO:
 * <ol>
 *   <li>{@code DataSource} — {@code @ConfigurationProperties("spring.datasource.postgres")}
 *       + {@code DataSourceBuilder}</li>
 *   <li>{@code JdbcTemplate(postgres)} — Spring AI {@code PgVectorStore} 가 사용</li>
 *   <li>{@code PgVectorStore} 빈 — 위 JdbcTemplate + {@code EmbeddingModel}(AWS Bedrock) 주입,
 *       {@code spring.ai.vectorstore.pgvector.*} 옵션 반영</li>
 *   <li>(선택) RAG 전용 MyBatis {@code SqlSessionFactory} / {@code @MapperScan}</li>
 * </ol>
 */
@Configuration
public class RagDataSourceConfig {
    // TODO: 위 주석의 빈 정의를 채운다.
}
