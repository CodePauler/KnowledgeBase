package com.knowledgebase.backend.entity;

/**
 * @description: 知识空间
 * @date: 2025/12/13 21:59
 */
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Space {
    private Long id;                    // 空间id
    private Long userId;                // 所有者id
    private String name;                // 空间名称
    private String description;         // 空间描述
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 创建时间
}
