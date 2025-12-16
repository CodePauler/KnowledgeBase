package com.knowledgebase.backend.dto;

/**
 * @description: TODO
 * @date: 2025/12/13 23:56
 */
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceCreateResponseDto {
    private String name;
    private String description;
}
