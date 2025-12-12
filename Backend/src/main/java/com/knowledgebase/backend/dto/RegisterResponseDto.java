package com.knowledgebase.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @description: TODO
 * @date: 2025/12/12 22:19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponseDto {
    private Long id;
    private String username;
    private String email;
}
