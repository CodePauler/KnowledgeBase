package com.knowledgebase.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话消息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    /**
     * 消息角色：user / assistant / system
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息时间戳
     */
    private Long timestamp;
}
