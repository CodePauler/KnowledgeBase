package com.knowledgebase.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeUpdateRequestDto {
    private String title;   // 知识名
    private String content; // 知识内容
    private Long parentId;  // 父知识id
    private String ossKey;  // osskey
}