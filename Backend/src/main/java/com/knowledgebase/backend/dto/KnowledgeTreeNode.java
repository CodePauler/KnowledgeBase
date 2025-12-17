package com.knowledgebase.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeTreeNode {
    private Long id;
    private Long spaceId;
    private String title;
    private String type;
    private Long parentId;

    @Builder.Default // Builder注解不会显示初始化children为ArrayList，需要加此注解
    private List<KnowledgeTreeNode> children = new ArrayList<>();
}