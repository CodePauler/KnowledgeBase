package com.knowledgebase.backend.service;

import com.knowledgebase.backend.dto.ChatRequestDto;
import com.knowledgebase.backend.dto.ChatResponseDto;
import com.knowledgebase.backend.dto.KnowledgeSearchResultDto;
import com.knowledgebase.backend.dto.SourceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RAG 对话服务
 * 结合向量检索和大语言模型实现知识问答
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final KnowledgeService knowledgeService;
    private final ConversationManager conversationManager;
    private final ChatModel chatModel;

    /**
     * 系统 Prompt 模板
     */
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是一个专业的知识库助手。请根据以下参考资料回答用户的问题。

            **重要规则：**
            1. 只能基于提供的参考资料回答问题
            2. 如果参考资料中没有相关信息，请明确告知用户"抱歉，我在知识库中没有找到相关信息"
            3. 回答要准确、简洁、专业
            4. 可以引用资料中的原文，并说明来源
            5. 返回格式MARKDOWN

            **参考资料：**
            %s
            """;

    /**
     * 非流式对话（返回完整回答）
     */
    public ChatResponseDto chat(ChatRequestDto request) {
        // 1. 验证并准备会话 ID
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = conversationManager.createConversation();
        }

        // 2. 向量检索相关知识
        List<KnowledgeSearchResultDto> searchResults = knowledgeService.searchBySemantics(
                request.getQuestion(),
                request.getSpaceId(),
                request.getKnowledgeIds(),
                request.getTopK(),
                request.getSimilarityThreshold());

        if (searchResults.isEmpty()) {
            // 没有检索到相关知识
            return ChatResponseDto.builder()
                    .answer("抱歉，我在知识库中没有找到与您的问题相关的信息。")
                    .conversationId(conversationId)
                    .sources(List.of())
                    .retrievedCount(0)
                    .build();
        }

        // 3. 构造上下文
        String context = buildContext(searchResults, request.getChunksPerKnowledge());

        // 4. 构造系统消息（仅在新会话或首次使用知识库时添加）
        List<Message> history = conversationManager.getHistory(conversationId);
        if (history.isEmpty()) {
            conversationManager.addSystemMessage(conversationId,
                    String.format(SYSTEM_PROMPT_TEMPLATE, context));
        }

        // 5. 添加用户消息
        conversationManager.addUserMessage(conversationId, request.getQuestion());

        // 6. 调用大模型
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        String answer = chatClient.prompt()
                .messages(conversationManager.getHistory(conversationId))
                .call()
                .content();

        // 7. 保存 AI 回复到历史
        conversationManager.addAssistantMessage(conversationId, answer);

        // 8. 构造响应
        List<SourceDto> sources = buildSources(searchResults);

        return ChatResponseDto.builder()
                .answer(answer)
                .conversationId(conversationId)
                .sources(sources)
                .retrievedCount(searchResults.size())
                .build();
    }

    /**
     * 流式对话（逐字返回）
     */
    public Flux<String> chatStream(ChatRequestDto request) {
        // 1. 验证并准备会话 ID
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = conversationManager.createConversation();
        }
        final String finalConversationId = conversationId;

        // 2. 向量检索相关知识
        List<KnowledgeSearchResultDto> searchResults = knowledgeService.searchBySemantics(
                request.getQuestion(),
                request.getSpaceId(),
                request.getKnowledgeIds(),
                request.getTopK(),
                request.getSimilarityThreshold());

        if (searchResults.isEmpty()) {
            return Flux.just("抱歉，我在知识库中没有找到与您的问题相关的信息。");
        }

        // 3. 构造上下文
        String context = buildContext(searchResults, request.getChunksPerKnowledge());

        // 4. 构造系统消息
        List<Message> history = conversationManager.getHistory(finalConversationId);
        if (history.isEmpty()) {
            conversationManager.addSystemMessage(finalConversationId,
                    String.format(SYSTEM_PROMPT_TEMPLATE, context));
        }

        // 5. 添加用户消息
        conversationManager.addUserMessage(finalConversationId, request.getQuestion());

        // 6. 流式调用大模型
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        StringBuilder fullAnswer = new StringBuilder();

        return chatClient.prompt()
                .messages(conversationManager.getHistory(finalConversationId))
                .stream()
                .content()
                .doOnNext(fullAnswer::append)
                .doOnComplete(() -> {
                    // 流式完成后保存完整回复到历史
                    conversationManager.addAssistantMessage(finalConversationId, fullAnswer.toString());
                    log.info("Chat stream completed for conversation: {}", finalConversationId);
                })
                .doOnError(error -> {
                    log.error("Chat stream error for conversation: {}", finalConversationId, error);
                });
    }

    /**
     * 构造上下文字符串
     */
    private String buildContext(List<KnowledgeSearchResultDto> searchResults, Integer chunksPerKnowledge) {
        StringBuilder context = new StringBuilder();

        for (int i = 0; i < searchResults.size(); i++) {
            KnowledgeSearchResultDto result = searchResults.get(i);
            context.append(String.format("\n【资料%d：%s】\n", i + 1, result.getTitle()));

            // 取每个知识的前 N 个最相关片段（可配置）
            List<Document> topChunks = result.getChunks().stream()
                    .limit(chunksPerKnowledge != null ? chunksPerKnowledge : 5)
                    .toList();

            for (Document chunk : topChunks) {
                context.append(chunk.getText()).append("\n");
            }
        }

        return context.toString();
    }

    /**
     * 构造来源列表
     */
    private List<SourceDto> buildSources(List<KnowledgeSearchResultDto> searchResults) {
        return searchResults.stream()
                .map(result -> SourceDto.builder()
                        .knowledgeId(result.getKnowledgeId())
                        .title(result.getTitle())
                        .snippet(result.getChunks().isEmpty() ? ""
                                : result.getChunks().get(0).getText().substring(
                                        0, Math.min(200, result.getChunks().get(0).getText().length()))
                                        + "...")
                        .score(result.getAvgScore())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 清除会话
     */
    public void clearConversation(String conversationId) {
        conversationManager.clearConversation(conversationId);
    }

    /**
     * 简单LLM流式调用（不使用RAG）
     * 用于AI写作助手等不需要知识库检索的场景
     */
    public Flux<String> simpleLlmStream(String prompt) {
        log.info("Simple LLM stream called with prompt length: {}",
                prompt != null ? prompt.length() : 0);

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .doOnNext(content -> log.debug("LLM generated content chunk: {}", content))
                .map(content -> {
                    // 构造 SSE 格式，使用简单的转义
                    String escaped = content
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                            .replace("\t", "\\t");
                    return String.format("data: {\"content\": \"%s\"}\n\n", escaped);
                })
                .concatWith(Flux.just("data: [DONE]\n\n"))
                .doOnComplete(() -> log.info("Simple LLM stream completed"))
                .doOnError(e -> log.error("Error in simple LLM stream", e));
    }
}
