package com.ditto.config.persistence;

import org.springframework.context.annotation.Configuration;

/**
 * 메인 DB(Oracle) MyBatis 설정 — 구조 골격.
 *
 * <p>TODO: 아래 순서로 빈을 정의한다.
 * <ol>
 *   <li>{@code @Primary DataSource} — {@code @ConfigurationProperties("spring.datasource.oracle")}
 *       + {@code DataSourceBuilder} (Hikari, jdbc-url 바인딩)</li>
 *   <li>{@code SqlSessionFactory} — {@code SqlSessionFactoryBean} 에 위 DataSource +
 *       mapper-locations({@code classpath:mapper/**}) 주입</li>
 *   <li>{@code @MapperScan(basePackages = "com.ditto", sqlSessionFactoryRef = "oracleSqlSessionFactory")}</li>
 *   <li>{@code DataSourceTransactionManager} ({@code @Primary})</li>
 * </ol>
 */
@Configuration
public class OracleDataSourceConfig {
    // TODO: 위 주석의 빈 정의를 채운다. (JPA 미사용 — MyBatis 기반)
}
