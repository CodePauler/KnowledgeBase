package com.knowledgebase.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * 多数据源配置：MySQL（主） + PostgreSQL（向量存储）
 */
@Configuration
public class DataSourceConfig {

    /**
     * MySQL 数据源（主数据源，用于业务数据）
     */
    @Primary
    @Bean(name = "mysqlDataSource")
    public DataSource mysqlDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        // 明确指定 Hikari, 并给出 poolName 便于排查
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url(url)
                .username(username)
                .password(password)
                .build();
    }

    /**
     * PostgreSQL 数据源（用于向量存储）
     * 从 spring.ai.vectorstore.pgvector 配置中读取连接信息
     */
    @Bean(name = "postgresDataSource")
    public DataSource postgresDataSource(
            @Value("${spring.ai.vectorstore.pgvector.url}") String url,
            @Value("${spring.ai.vectorstore.pgvector.username}") String username,
            @Value("${spring.ai.vectorstore.pgvector.password}") String password,
            @Value("${spring.ai.vectorstore.pgvector.driver-class-name}") String driverClassName) {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName(driverClassName)
                .url(url)
                .username(username)
                .password(password)
                .build();
    }
}