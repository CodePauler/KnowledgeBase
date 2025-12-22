# RAG 知识问答系统 - 技术文档

## 1. 系统概述

本系统是一个基于 **RAG (Retrieval-Augmented Generation)** 架构的智能知识问答系统,结合了向量检索和大语言模型的能力,能够准确回答用户关于知识库的提问。

### 1.1 核心特性

- ✅ **混合存储架构**: MySQL 存储业务数据 + PostgreSQL 向量数据库存储嵌入向量
- ✅ **智能文档解析**: 支持 PDF、DOCX 等多种格式文档的自动解析
- ✅ **语义检索**: 基于 Embedding 的向量相似度搜索
- ✅ **上下文增强**: 检索相关知识片段增强大模型回答准确性
- ✅ **对话历史管理**: 支持多轮对话上下文保持
- ✅ **流式响应**: SSE (Server-Sent Events) 实现逐字返回
- ✅ **灵活过滤**: 支持按空间、知识 ID 等维度过滤检索结果

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────┐
│   用户请求   │ (问题、会话 ID、过滤条件)
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────┐
│         ChatController (REST API)        │
│  - POST /api/knowledge/chat              │
│  - POST /api/knowledge/chat/stream (SSE) │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│          ChatService (业务逻辑)          │
│  1. 向量检索 (searchBySemantics)        │
│  2. 上下文构造                           │
│  3. Prompt 构建                          │
│  4. 大模型调用                           │
│  5. 历史管理                             │
└─────┬────────────────────┬──────────────┘
      │                    │
      │                    ▼
      │         ┌──────────────────────┐
      │         │  ConversationManager  │
      │         │  (对话历史管理)       │
      │         │  - 内存缓存           │
      │         │  - 30分钟过期         │
      │         │  - 最多10条消息       │
      │         └──────────────────────┘
      │
      ▼
┌─────────────────────────────────────────┐
│      KnowledgeService (知识检索)         │
│  - 向量相似度搜索                        │
│  - 结果分组与排序                        │
│  - 知识元数据加载                        │
└─────┬───────────────────┬───────────────┘
      │                   │
      ▼                   ▼
┌──────────────┐   ┌──────────────────┐
│ VectorStore  │   │  KnowledgeMapper │
│ (PostgreSQL) │   │     (MySQL)      │
│ - pgvector   │   │  - 知识元数据    │
│ - 向量索引   │   │  - 用户数据      │
└──────────────┘   └──────────────────┘
```

### 2.2 数据流向

#### 文档上传与向量化流程

```
1. 用户上传文档 (PDF/DOCX)
   ↓
2. FileUploadController 处理请求
   ↓
3. 保存文件到 Azure Blob Storage
   ↓
4. DocumentParseService 解析文档
   ↓
5. Apache Tika 提取文本内容
   ↓
6. TokenTextSplitter 分块 (800 tokens, 重叠 400)
   ↓
7. ZhiPuAI embedding-3 模型生成向量 (1024维)
   ↓
8. 保存到 PostgreSQL pgvector
   ↓
9. 元数据保存到 MySQL (knowledge 表)
```

#### RAG 问答流程

```
1. 用户提问
   ↓
2. ChatController 接收请求
   ↓
3. ChatService 调用 searchBySemantics
   ↓
4. 向量化用户问题 (embedding-3)
   ↓
5. PostgreSQL 余弦相似度检索 (TopK)
   ↓
6. 分组聚合检索结果 (按 knowledgeId)
   ↓
7. MySQL 加载知识元数据 (标题、类型)
   ↓
8. 构造上下文 Prompt (系统指令 + 参考资料)
   ↓
9. 加载对话历史 (ConversationManager)
   ↓
10. 调用 GLM-4-Flash 生成回答
   ↓
11. 保存回复到历史
   ↓
12. 返回结果给用户 (流式或非流式)
```

---

## 3. 核心组件详解

### 3.1 数据源配置 (DataSourceConfig)

```java
@Configuration
public class DataSourceConfig {
    // MySQL 数据源 (业务数据)
    @Bean
    @Primary
    public DataSource mysqlDataSource() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .url(mysqlUrl)
            .username(mysqlUsername)
            .password(mysqlPassword)
            .build();
    }
    
    // PostgreSQL 数据源 (向量存储)
    @Bean(name = "postgresDataSource")
    public DataSource postgresDataSource() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .url(postgresUrl)
            .username(postgresUsername)
            .password(postgresPassword)
            .build();
    }
}
```

**关键点:**

- 使用 HikariCP 连接池提升性能
- MySQL 主数据源用于 MyBatis 业务操作
- PostgreSQL 专用于向量存储

### 3.2 向量存储配置 (VectorStoreConfig)

```java
@Configuration
public class VectorStoreConfig {
    @Bean
    public VectorStore vectorStore(
        @Qualifier("postgresDataSource") DataSource dataSource,
        EmbeddingModel embeddingModel) {
        
        return PgVectorStore.builder()
            .dataSource(dataSource)
            .embeddingModel(embeddingModel)
            .dimensions(1024)  // embedding-3 输出维度
            .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
            .removeExistingVectorStoreTable(false)
            .initializeSchema(true)
            .build();
    }
}
```

**关键点:**

- 使用余弦距离 (COSINE_DISTANCE) 度量相似度
- 自动初始化表结构 (vector_store 表)
- 1024 维向量空间

### 3.3 文档解析服务 (DocumentParseService)

```java
@Service
public class DocumentParseService {
    // 解析文档并向量化
    public void parseAndEmbed(Long knowledgeId, String fileUrl) {
        // 1. 下载文件
        byte[] fileBytes = fileStorageInterface.downloadFile(fileUrl);
        
        // 2. 使用 Tika 解析
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.get();
        
        // 3. 分块处理
        TokenTextSplitter splitter = new TokenTextSplitter(800, 400, 5, 10000, true);
        List<Document> chunks = splitter.apply(documents);
        
        // 4. 清理元数据并添加 knowledgeId
        chunks.forEach(doc -> {
            doc.getMetadata().clear();
            doc.getMetadata().put("knowledgeId", knowledgeId.toString());
        });
        
        // 5. 存入向量数据库
        vectorStore.add(chunks);
        
        // 6. 保存内容到 MySQL
        knowledge.setContent(fullContent);
        knowledgeMapper.updateById(knowledge);
    }
    
    // Markdown 向量化
    public void embedMarkdown(Long knowledgeId, String content) {
        // 直接对 Markdown 内容分块嵌入
        Document doc = new Document(content);
        TokenTextSplitter splitter = new TokenTextSplitter(800, 400, 5, 10000, true);
        List<Document> chunks = splitter.apply(List.of(doc));
        
        chunks.forEach(chunk -> 
            chunk.getMetadata().put("knowledgeId", knowledgeId.toString())
        );
        
        vectorStore.add(chunks);
    }
}
```

**关键点:**

- 支持 DOC (上传文档) 和 MANUAL (手动编辑) 类型
- 统一使用 800 token 分块,400 token 重叠
- 元数据只保留 knowledgeId,避免污染

### 3.4 知识检索服务 (KnowledgeService.searchBySemantics)

```java
public List<KnowledgeSearchResultDto> searchBySemantics(
    String question, 
    Long spaceId,
    List<Long> knowledgeIds,
    Integer topK,
    Double similarityThreshold) {
    
    // 1. 构造过滤表达式
    String filterExpression = buildFilterExpression(spaceId, knowledgeIds);
    
    // 2. 向量检索
    SearchRequest request = SearchRequest.builder()
        .query(question)
        .topK(topK)
        .similarityThreshold(similarityThreshold)
        .filterExpression(filterExpression)
        .build();
    
    List<Document> results = vectorStore.similaritySearch(request);
    
    // 3. 分组聚合 (按 knowledgeId)
    Map<Long, List<Document>> grouped = results.stream()
        .collect(Collectors.groupingBy(doc -> 
            Long.parseLong(doc.getMetadata().get("knowledgeId").toString())
        ));
    
    // 4. 批量加载知识元数据
    List<Long> ids = new ArrayList<>(grouped.keySet());
    List<Knowledge> knowledges = knowledgeMapper.selectByIds(ids);
    
    // 5. 构造结果 DTO
    return knowledges.stream()
        .map(knowledge -> {
            List<Document> chunks = grouped.get(knowledge.getId());
            double avgScore = chunks.stream()
                .mapToDouble(doc -> (Double) doc.getMetadata().get("distance"))
                .average()
                .orElse(0.0);
            
            return KnowledgeSearchResultDto.builder()
                .knowledgeId(knowledge.getId())
                .title(knowledge.getTitle())
                .type(knowledge.getType())
                .chunks(chunks)
                .avgScore(avgScore)
                .build();
        })
        .sorted(Comparator.comparingDouble(
            KnowledgeSearchResultDto::getAvgScore).reversed())
        .collect(Collectors.toList());
}
```

**关键点:**

- 支持按空间、知识 ID 过滤
- 将检索结果按知识文档分组
- 计算每个文档的平均相似度分数
- 按分数排序返回

### 3.5 对话历史管理 (ConversationManager)

```java
@Service
public class ConversationManager {
    private final Cache<String, List<Message>> conversationCache;
    
    public ConversationManager() {
        this.conversationCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(1000)
            .build();
    }
    
    public void addUserMessage(String conversationId, String content) {
        List<Message> history = getOrCreateHistory(conversationId);
        history.add(new UserMessage(content));
        
        // 限制最多保留 10 条消息
        if (history.size() > 10) {
            history.remove(1); // 保留 system message
        }
    }
}
```

**关键点:**

- 使用 Caffeine 内存缓存
- 30 分钟不活跃自动过期
- 每个会话最多 10 条消息
- 保留 system message (RAG prompt)

### 3.6 RAG 对话服务 (ChatService)

#### 系统 Prompt 设计

```java
private static final String SYSTEM_PROMPT_TEMPLATE = """
    你是一个专业的知识库助手。请根据以下参考资料回答用户的问题。
    
    **重要规则：**
    1. 只能基于提供的参考资料回答问题
    2. 如果参考资料中没有相关信息,请明确告知用户"抱歉,我在知识库中没有找到相关信息"
    3. 回答要准确、简洁、专业
    4. 可以引用资料中的原文,并说明来源
    
    **参考资料：**
    %s
    """;
```

#### 非流式对话

```java
public ChatResponseDto chat(ChatRequestDto request) {
    // 1. 创建或获取会话 ID
    String conversationId = getOrCreateConversationId(request);
    
    // 2. 向量检索
    List<KnowledgeSearchResultDto> searchResults = 
        knowledgeService.searchBySemantics(...);
    
    // 3. 构造上下文
    String context = buildContext(searchResults);
    
    // 4. 添加系统消息 (首次)
    if (isNewConversation) {
        conversationManager.addSystemMessage(conversationId, 
            String.format(SYSTEM_PROMPT_TEMPLATE, context));
    }
    
    // 5. 添加用户消息
    conversationManager.addUserMessage(conversationId, question);
    
    // 6. 调用大模型
    String answer = chatClient.prompt()
        .messages(conversationManager.getHistory(conversationId))
        .call()
        .content();
    
    // 7. 保存回复到历史
    conversationManager.addAssistantMessage(conversationId, answer);
    
    // 8. 返回结果
    return new ChatResponseDto(answer, conversationId, sources, count);
}
```

#### 流式对话

```java
public Flux<String> chatStream(ChatRequestDto request) {
    // ... 前面步骤同非流式 ...
    
    StringBuilder fullAnswer = new StringBuilder();
    
    return chatClient.prompt()
        .messages(conversationManager.getHistory(conversationId))
        .stream()
        .content()
        .doOnNext(fullAnswer::append)  // 累积完整回复
        .doOnComplete(() -> {
            // 流式完成后保存到历史
            conversationManager.addAssistantMessage(
                conversationId, fullAnswer.toString());
        });
}
```

**关键点:**

- 使用 Reactor Flux 实现响应式流
- 累积完整回复用于历史保存
- doOnComplete 确保流结束后保存

---

## 4. API 接口说明

### 4.1 非流式对话

**请求:**

```http
POST /api/knowledge/chat
Content-Type: application/json

{
  "question": "什么是 RAG?",
  "conversationId": "uuid-xxx",  // 可选,不传则创建新会话
  "spaceId": 1,                   // 可选,按空间过滤
  "knowledgeIds": [1, 2, 3],      // 可选,按知识 ID 过滤
  "topK": 5,                      // 可选,默认 5
  "similarityThreshold": 0.5      // 可选,默认 0.5
}
```

**响应:**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "answer": "RAG 是 Retrieval-Augmented Generation 的缩写...",
    "conversationId": "uuid-xxx",
    "sources": [
      {
        "knowledgeId": 1,
        "title": "RAG 技术介绍",
        "snippet": "RAG 结合了检索和生成...",
        "score": 0.89
      }
    ],
    "retrievedCount": 2
  }
}
```

### 4.2 流式对话

**请求:**

```http
POST /api/knowledge/chat/stream
Content-Type: application/json
Accept: text/event-stream

{
  "question": "什么是 RAG?",
  "conversationId": "uuid-xxx"
}
```

**响应 (SSE 流):**

```
data: RAG
data:  是
data:  Retrieval
data: -Augmented
data:  Generation
data:  的
data:  缩写
data: ...
```

### 4.3 清除对话历史

**请求:**

```http
DELETE /api/knowledge/chat/{conversationId}
```

---

## 5. 配置说明

### 5.1 application.yml

```yaml
# MySQL 配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/knowledge_base
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

# PostgreSQL 配置
postgres:
  datasource:
    url: jdbc:postgresql://localhost:5432/vector_db
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver

# ZhiPuAI 配置
spring:
  ai:
    zhipuai:
      api-key: ${ZHIPUAI_API_KEY}
      embedding:
        options:
          model: embedding-3  # 1024 维向量
      chat:
        options:
          model: glm-4-flash
          temperature: 0.7
          max-tokens: 2000

# 向量化配置
knowledge:
  vector:
    min-length: 100  # 最小向量化长度

# Azure Blob Storage
azure:
  storage:
    connection-string: ${AZURE_STORAGE_CONNECTION_STRING}
    container-name: knowledge-files
```

### 5.2 环境变量

```bash
# ZhiPuAI API Key
export ZHIPUAI_API_KEY=your_api_key

# Azure Storage
export AZURE_STORAGE_CONNECTION_STRING=your_connection_string
```

---

## 6. 向量存储表结构

### PostgreSQL vector_store 表

```sql
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT,                -- 文本内容
    metadata JSONB,              -- 元数据 (包含 knowledgeId)
    embedding vector(1024)       -- 1024 维向量
);

-- 创建向量索引 (提升检索性能)
CREATE INDEX ON vector_store 
USING ivfflat (embedding vector_cosine_ops) 
WITH (lists = 100);
```

**元数据结构:**

```json
{
  "knowledgeId": "123",
  "distance": 0.85  // 检索时添加
}
```

---

## 7. 性能优化

### 7.1 向量检索优化

- **IVFFlat 索引**: 使用近似最近邻搜索,牺牲少量精度换取速度
- **余弦距离**: 归一化后的点积,计算效率高
- **TopK 限制**: 默认只检索 Top 5,减少后续处理开销

### 7.2 连接池配置

- **HikariCP**: 高性能 JDBC 连接池
- **最大连接数**: 根据并发量调整 (默认 10)
- **连接超时**: 30 秒

### 7.3 缓存策略

- **对话历史**: 内存缓存,30 分钟过期
- **向量缓存**: PostgreSQL 自带查询缓存
- **知识元数据**: 可考虑添加 Redis 缓存 (未实现)

---

## 8. 常见问题 (FAQ)

### Q1: 为什么需要双数据库?

**A**:

- **MySQL**: 擅长结构化数据存储和复杂关联查询,用于业务数据
- **PostgreSQL + pgvector**: 原生支持向量类型和相似度检索,用于语义搜索

### Q2: 为什么要保存 content 到 MySQL?

**A**:

- 向量数据库只存储分块后的片段,不适合展示完整内容
- 用户需要查看完整文档原文
- 混合检索:可结合关键词和向量检索

### Q3: 对话历史为什么不持久化?

**A**:

- 当前是 MVP 阶段,优先实现核心功能
- 内存缓存性能更高,适合短期会话
- 未来可扩展到 Redis 或数据库持久化

### Q4: 如何防止幻觉 (Hallucination)?

**A**:

- 系统 Prompt 明确要求"只能基于参考资料回答"
- 检索不到相关知识时返回固定提示
- 可考虑添加答案验证机制 (未实现)

### Q5: 如何提升检索准确性?

**A**:

- 调整 `similarityThreshold` (相似度阈值)
- 增大 `topK` 值获取更多候选
- 优化文档分块策略 (调整 chunk size 和 overlap)
- 使用混合检索 (向量 + 关键词)

---

## 9. 扩展建议

### 9.1 短期优化

- [ ] 添加答案评分机制 (用户反馈)
- [ ] 实现混合检索 (BM25 + 向量)
- [ ] 添加 Redis 缓存层
- [ ] 对话历史持久化到数据库

### 9.2 长期规划

- [ ] 多模态支持 (图片、表格)
- [ ] 知识图谱增强
- [ ] 个性化 Prompt (用户偏好)
- [ ] 分布式向量存储 (Milvus/Qdrant)
- [ ] RAG 评估指标 (Relevance/Faithfulness)

---

## 10. 参考资料

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [pgvector GitHub](https://github.com/pgvector/pgvector)
- [ZhiPuAI API 文档](https://open.bigmodel.cn/dev/api)
- [RAG 论文](https://arxiv.org/abs/2005.11401)

---

**文档版本**: v1.0  
**最后更新**: 2025-01-08  
**作者**: KnowledgeBase Team
