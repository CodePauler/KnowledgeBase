package com.knowledgebase.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 知识检索结果 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeSearchResultDto {

    /**
     * 知识 ID
     */
    private Long knowledgeId;

    /**
     * 知识标题
     */
    private String title;

    /**
     * 知识类型
     */
    private String type;

    /**
     * 匹配的文档片段列表
     */
    private List<Document> chunks;

    /**
     * 平均相似度分数
     */
    private Double avgScore;
}
