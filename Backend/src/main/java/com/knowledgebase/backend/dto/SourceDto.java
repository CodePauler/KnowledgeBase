package com.knowledgebase.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识来源 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceDto {
    
    /**
     * 知识 ID
     */
    private Long knowledgeId;
    
    /**
     * 知识标题
     */
    private String title;
    
    /**
     * 匹配的文档片段
     */
    private String snippet;
    
    /**
     * 相似度分数
     */
    private Double score;
}
