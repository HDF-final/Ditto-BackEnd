package com.ditto.config.persistence;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

/**
 * 메인 DB(Oracle) MyBatis 및 DataSource 설정.
 */
@Configuration
@MapperScan(
        basePackages = {
                "com.ditto.country.repository",
                "com.ditto.user.repository"
        },
        sqlSessionFactoryRef = "oracleSqlSessionFactory")
public class OracleDataSourceConfig {

    @Primary
    @Bean(name = "oracleDataSource")
    @ConfigurationProperties("spring.datasource.oracle")
    public DataSource oracleDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Primary
    @Bean(name = "oracleSqlSessionFactory")
    public SqlSessionFactory oracleSqlSessionFactory(DataSource oracleDataSource) throws Exception {
        SqlSessionFactoryBean sessionFactoryBean = new SqlSessionFactoryBean();
        sessionFactoryBean.setDataSource(oracleDataSource);

        org.apache.ibatis.session.Configuration mybatisConfig = new org.apache.ibatis.session.Configuration();
        mybatisConfig.setMapUnderscoreToCamelCase(true);
        sessionFactoryBean.setConfiguration(mybatisConfig);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactoryBean.setMapperLocations(resolver.getResources("classpath*:mapper/**/*.xml"));

        return sessionFactoryBean.getObject();
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager oracleTransactionManager(DataSource oracleDataSource) {
        return new DataSourceTransactionManager(oracleDataSource);
    }
}
