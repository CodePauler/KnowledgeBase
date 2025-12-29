package com.knowledgebase.backend.controller;

import com.knowledgebase.backend.dto.ChatRequestDto;
import com.knowledgebase.backend.dto.ChatResponseDto;
import com.knowledgebase.backend.entity.Result;
import com.knowledgebase.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * RAG 对话接口
 */
@RestController
@RequestMapping("/api/knowledge/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * 非流式对话
     * POST /api/knowledge/chat
     */
    @PostMapping
    public Result<ChatResponseDto> chat(@RequestBody ChatRequestDto request) {
        log.info("Received chat request: question={}, spaceId={}, conversationId={}",
                request.getQuestion(), request.getSpaceId(), request.getConversationId());

        ChatResponseDto response = chatService.chat(request);

        return Result.success(response);
    }

    /**
     * 流式对话（SSE）
     * POST /api/knowledge/chat/stream
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequestDto request) {
        log.info("Received chat stream request: question={}, spaceId={}, conversationId={}",
                request.getQuestion(), request.getSpaceId(), request.getConversationId());

        return chatService.chatStream(request)
                .doOnSubscribe(subscription -> log.info("Client subscribed to chat stream"))
                .doOnComplete(() -> log.info("Chat stream completed"))
                .doOnError(error -> log.error("Chat stream error", error));
    }

    /**
     * 清除对话历史
     * DELETE /api/knowledge/chat/{conversationId}
     */
    @DeleteMapping("/{conversationId}")
    public Result<Void> clearConversation(@PathVariable String conversationId) {
        log.info("Clearing conversation: {}", conversationId);
        chatService.clearConversation(conversationId);
        return Result.success(null, "success");
    }

    /**
     * 简单LLM调用（不使用RAG，用于AI写作助手）
     * POST /api/knowledge/chat/simple-stream
     */
    @PostMapping(value = "/simple-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> simpleStream(@RequestBody @Validated ChatRequestDto request) {
        log.info("Received simple LLM stream request: question length={}",
                request.getQuestion() != null ? request.getQuestion().length() : 0);

        return chatService.simpleLlmStream(request.getQuestion())
                .doOnSubscribe(subscription -> log.info("Client subscribed to simple LLM stream"))
                .doOnComplete(() -> log.info("Simple LLM stream completed"))
                .doOnError(error -> log.error("Simple LLM stream error", error));
    }
}
