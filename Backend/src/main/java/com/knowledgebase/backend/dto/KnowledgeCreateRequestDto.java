package com.knowledgebase.backend.dto;

import com.knowledgebase.backend.entity.KnowledgeType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeCreateRequestDto {
    private Long spaceId;
    private String title;
    private KnowledgeType type;   // DOC / MANUAL
    private String content;       // MANUAL: markdown；DOC: 解析纯文本(可后续补)
    private Long parentId;        // 可空：顶层
    private String blobKey;       // DOC 用（可空：先创建后上传也行）
}
