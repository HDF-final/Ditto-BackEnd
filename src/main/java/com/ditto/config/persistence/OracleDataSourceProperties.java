package com.ditto.config.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code spring.datasource.oracle.*} 바인딩.
 * 값은 {@code .env} 의 ORACLE_* 를 application.yml 의 {@code ${ORACLE_*}} 가 채운다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.datasource.oracle")
public class OracleDataSourceProperties {

    private String jdbcUrl;
    private String username;
    private String password;
    private String driverClassName;
}
