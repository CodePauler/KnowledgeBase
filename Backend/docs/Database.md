1. user（用户）
   - id, username, password, email, created_at

2. space（知识库空间）
   - id, user_id（谁创建的）, name（名字）, description, created_at

3. knowledge（知识条目）
   - id, space_id（属于哪个空间）, title（标题）
   - type（文档上传 / 手工录入）
   - content（手工录入的内容，文档上传时为空）
   - parent_id（父知识，用于层级，为空表示顶层）
   - oss_key（文件在 OSS 的位置，文档上传时填）
   - created_at, updated_at

4. knowledge_embedding（知识向量，用于 RAG）
   - id, knowledge_id（关联哪个知识）
   - text_chunk（分割的文本片段，"第一段内容..."）
   - embedding（向量值，用于相似搜索）
   - created_at