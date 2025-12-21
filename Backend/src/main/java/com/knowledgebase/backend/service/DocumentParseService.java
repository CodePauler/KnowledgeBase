package com.knowledgebase.backend.service;

import com.knowledgebase.backend.dao.KnowledgeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentParseService {

    private final FileStorageInterface fileStorageService;
    private final KnowledgeMapper knowledgeMapper;
    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = new TokenTextSplitter();
    
    @Value("${knowledge.vector.min-length:100}")
    private int minVectorLength;

    @Async // 异步解析，不阻塞主流程
    public void parseAndEmbed(Long knowledgeId, String blobKey) {
        knowledgeMapper.updateParseJob(knowledgeId, "RUNNING");
        try (InputStream inputStream = fileStorageService.download(blobKey).getStream()) {
            TikaDocumentReader reader = new TikaDocumentReader(new InputStreamResource(inputStream));
            List<Document> rawDocs = reader.read();

            List<Document> cleanDocs = new java.util.ArrayList<>();
            StringBuilder merged = new StringBuilder();

            for (Document d : rawDocs) {
                String text = stripInvalidSource(d.getFormattedContent());
                if (text.isBlank()) {
                    continue; // 跳过空文档
                }
                merged.append(text).append("\n\n");

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("knowledgeId", knowledgeId);
                metadata.put("blobKey", blobKey);
                metadata.put("filename", extractFilename(blobKey));
                cleanDocs.add(new Document(text, metadata));
            }

            if (!cleanDocs.isEmpty()) {
                List<Document> chunks = splitter.split(cleanDocs);
                vectorStore.add(chunks);
            }
            if (merged.length() > 0) {
                knowledgeMapper.updateById(knowledgeId, null, merged.toString().trim(), null, null);
            }
            log.info("Parse and embed succeeded for knowledgeId={}, blobKey={}", knowledgeId, blobKey);
            knowledgeMapper.updateParseJob(knowledgeId, "DONE");
        } catch (Exception e) {
            log.error("Parse and embed failed for knowledgeId={}, blobKey={}", knowledgeId, blobKey, e);
            knowledgeMapper.updateParseJob(knowledgeId, "FAILED");
        }
    }

    private String stripInvalidSource(String text) {
        if (text == null)
            return "";
        // 去掉 Tika 在 InputStreamResource 场景下生成的错误 source 行
        return text.replaceFirst("^source:\\s*Invalid source URI:.*(?:\\r?\\n)?", "").trim();
    }

    private String extractFilename(String blobKey) {
        if (blobKey == null || blobKey.isBlank())
            return "file";
        int idx = blobKey.lastIndexOf('/');
        return (idx >= 0 && idx < blobKey.length() - 1) ? blobKey.substring(idx + 1) : blobKey;
    }

    /**
     * 将手工录入的 Markdown 文本参与向量检索：
     * - 短文本（< minVectorLength）作为单块写入向量库
     * - 长文本按 TokenTextSplitter 分片后写入
     * 不涉及 parse_job 状态机更新，仅负责向量化。
     */
    public void embedMarkdown(Long knowledgeId, String content) {
        try {
            if (content == null || content.isBlank())
                return;

            String text = content.trim();

            // 构造元信息，便于后续按 knowledgeId 过滤
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("knowledgeId", knowledgeId);
            metadata.put("source", "markdown");

            List<Document> docs;
            if (text.length() < minVectorLength) {
                docs = List.of(new Document(text, metadata));
            } else {
                List<Document> base = List.of(new Document(text, metadata));
                docs = splitter.split(base);
            }

            if (!docs.isEmpty()) {
                vectorStore.add(docs);
            }
        } catch (Exception e) {
            log.error("Embed markdown failed for knowledgeId={}", knowledgeId, e);
        }
    }

    /**
     * 删除指定 knowledgeId 的所有向量分块
     * 用于更新或删除知识时清理旧的向量索引
     */
    public void deleteVectorsByKnowledgeId(Long knowledgeId) {
        try {
            Filter.Expression filter = new FilterExpressionBuilder()
                .eq("knowledgeId", knowledgeId)
                .build();
            vectorStore.delete(filter);
            log.info("Deleted vectors for knowledgeId={}", knowledgeId);
        } catch (Exception e) {
            log.error("Failed to delete vectors for knowledgeId={}", knowledgeId, e);
        }
    }
}
