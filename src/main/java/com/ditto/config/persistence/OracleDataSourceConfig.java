package com.ditto.config.persistence;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

/**
 * 메인 DB(Oracle) DataSource. MyBatis 는 이 {@code @Primary} DataSource 를 사용한다.
 * 접속 정보는 {@link OracleDataSourceProperties} ({@code spring.datasource.oracle.*}) 에서 읽는다.
 */
@Configuration
@EnableConfigurationProperties(OracleDataSourceProperties.class)
public class OracleDataSourceConfig {

    @Bean
    @Primary
    public DataSource oracleDataSource(OracleDataSourceProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.getJdbcUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setDriverClassName(properties.getDriverClassName());
        return dataSource;
    }

    @Bean
    @Primary
    public DataSourceTransactionManager oracleTransactionManager(DataSource oracleDataSource) {
        return new DataSourceTransactionManager(oracleDataSource);
    }
}
