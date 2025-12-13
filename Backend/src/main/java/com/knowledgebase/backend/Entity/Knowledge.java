package com.knowledgebase.backend.entity;

/**
 * @description: 知识实体类
 * @date: 2025/12/13 22:02
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
public class Knowledge {
    private Long id;                    // 知识id
    private Long spaceId;               // 所属空间id
    private String title;               // 知识标题
    private String type;                // 知识类型 DOC_UNSTRUCTED DOC_STRUCTED MANUAL_STRUCTED
    private String content;             // 知识内容（手工录入类知识）
    private Long parentId;              // 父知识id
    private String ossKey;              // OSS文件路径
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
}
