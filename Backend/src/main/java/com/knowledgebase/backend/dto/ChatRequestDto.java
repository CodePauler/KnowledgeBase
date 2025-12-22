package com.knowledgebase.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 对话请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {

    /**
     * 用户问题
     */
    private String question;

    /**
     * 会话 ID（用于多轮对话）
     * 如果为空则创建新会话
     */
    private String conversationId;

    /**
     * 限定检索的空间 ID（可选）
     */
    private Long spaceId;

    /**
     * 限定检索的知识 ID 列表（可选）
     */
    private List<Long> knowledgeIds;

    /**
     * 返回最相关的文档片段数量（默认 5）
     */
    @Builder.Default
    private Integer topK = 5;

    /**
     * 相似度阈值（0-1，默认 0.7）
     */
    @Builder.Default
    private Double similarityThreshold = 0.7;

    /**
     * 每个知识返回的最大 chunk 数量（默认 5）
     * 基于分析：TokenTextSplitter 默认 ~800 tokens/chunk
     * GLM-4-Flash 上下文 128k tokens
     * topK=5 * chunksPerKnowledge=5 * 800 = 20k tokens（参考资料）
     * 留余 108k tokens 给对话历史 + AI 回复
     */
    @Builder.Default
    private Integer chunksPerKnowledge = 5;
}
