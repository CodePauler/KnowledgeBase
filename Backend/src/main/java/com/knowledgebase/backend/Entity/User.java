package com.knowledgebase.backend.entity;

/*
  用户实体类，未来可能添加tenant架构
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
public class User {
    private Long id;
    private String username;
    private String password; // Bcrypt加密
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
