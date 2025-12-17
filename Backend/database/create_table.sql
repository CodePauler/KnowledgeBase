CREATE DATABASE IF NOT EXISTS `knowledgebase`;
USE `knowledgebase`;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
  `email` VARCHAR(100) COMMENT '邮箱',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 知识库空间表
CREATE TABLE IF NOT EXISTS `space` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '空间ID',
  `user_id` BIGINT NOT NULL COMMENT '所有者ID',
  `name` VARCHAR(100) NOT NULL COMMENT '空间名称',
  `description` TEXT COMMENT '空间描述',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 知识条目表
CREATE TABLE IF NOT EXISTS `knowledge` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '知识ID',
  `space_id` BIGINT NOT NULL COMMENT '所属空间ID',
  `title` VARCHAR(255) NOT NULL COMMENT '知识标题',
  `type` ENUM('DOC_UNSTRUCTURED', 'DOC_STRUCTURED', 'MANUAL_STRUCTURED') DEFAULT 'MANUAL_STRUCTURED' COMMENT '知识类型',
  `content` LONGTEXT COMMENT '知识内容(手工录入)',
  `parent_id` BIGINT COMMENT '父知识ID(层级)',
  `blob_key` VARCHAR(500) COMMENT 'blob name',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (space_id) REFERENCES `space`(id) ON DELETE CASCADE,
  FOREIGN KEY (parent_id) REFERENCES `knowledge`(id) ON DELETE CASCADE,
  INDEX idx_space_parent (space_id, parent_id),
  FULLTEXT INDEX ft_title_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 知识向量表(用于RAG)
CREATE TABLE IF NOT EXISTS `knowledge_embedding` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '向量ID',
  `knowledge_id` BIGINT NOT NULL COMMENT '知识ID',
  `text_chunk` LONGTEXT NOT NULL COMMENT '文本片段',
  `embedding` MEDIUMBLOB COMMENT '向量值(存储为二进制或JSON)',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (knowledge_id) REFERENCES `knowledge`(id) ON DELETE CASCADE,
  INDEX idx_knowledge_id (knowledge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- chunk表
CREATE TABLE IF NOT EXISTS `knowledge_chunk` (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'CHUNK_ID',
  knowledge_id BIGINT NOT NULL COMMENT '所属知识ID',
  chunk_index INT NOT NULL COMMENT 'chunk index',
  text LONGTEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (knowledge_id) REFERENCES knowledge(id) ON DELETE CASCADE,
  INDEX idx_kid_chunk (knowledge_id, chunk_index),
  FULLTEXT INDEX ft_chunk_text (text)
);

  
DROP TABLE IF EXISTS knowledge_embedding;
CREATE TABLE knowledge_embedding (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '向量ID',
  chunk_id BIGINT NOT NULL COMMENT 'chunk ID',
  embedding MEDIUMBLOB NOT NULL COMMENT '向量值(二进制/JSON均可)',
  model VARCHAR(100) DEFAULT 'text-embedding-3-small' COMMENT 'embedding模型标识',
  dim INT DEFAULT NULL COMMENT '向量维度',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (chunk_id) REFERENCES knowledge_chunk(id) ON DELETE CASCADE,
  UNIQUE KEY uk_chunk_model (chunk_id, model),
  INDEX idx_chunk_id (chunk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
