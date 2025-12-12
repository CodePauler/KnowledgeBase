package com.knowledgebase.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: TODO
 * @date: 2025/12/12 22:16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {
    private String token;
    private Long userId;
    private String username;
    // 将来可扩展字段，例如邮箱、角色、权限等
}
