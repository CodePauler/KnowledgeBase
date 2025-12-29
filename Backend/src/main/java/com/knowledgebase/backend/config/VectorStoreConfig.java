package com.knowledgebase.backend.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 向量存储配置
 * 使用 PostgreSQL 作为向量数据库
 */
@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.pgvector.dimension:2048}")
    private int dimensions;

    @Bean
    public VectorStore vectorStore(
            @Qualifier("postgresDataSource") DataSource postgresDataSource,
            EmbeddingModel embeddingModel) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(postgresDataSource);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(dimensions)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .initializeSchema(true)
                .build();
    }
}
