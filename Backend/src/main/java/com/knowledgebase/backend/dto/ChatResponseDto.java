package com.knowledgebase.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 对话响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {

    /**
     * AI 生成的回答
     */
    private String answer;

    /**
     * 会话 ID
     */
    private String conversationId;

    /**
     * 引用的知识来源
     */
    private List<SourceDto> sources;

    /**
     * 检索到的相关片段数量
     */
    private Integer retrievedCount;
}
