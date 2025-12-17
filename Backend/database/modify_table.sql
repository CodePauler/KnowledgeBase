USE knowledgebase;
ALTER TABLE knowledge
  MODIFY type ENUM('DOC','MANUAL') NOT NULL DEFAULT 'MANUAL',
  MODIFY content LONGTEXT COMMENT '知识内容：MANUAL=Markdown；DOC=解析出的纯文本';
  
ALTER TABLE knowledge RENAME COLUMN oss_key TO blob_key;
