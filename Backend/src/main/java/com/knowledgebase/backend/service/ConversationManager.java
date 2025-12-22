package com.knowledgebase.backend.service;

import com.knowledgebase.backend.dto.MessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话历史管理器
 * 使用内存缓存管理多轮对话历史
 */
@Component
@Slf4j
public class ConversationManager {

    /**
     * 对话历史存储：conversationId -> 消息列表
     */
    private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();

    /**
     * 对话最后活跃时间：conversationId -> timestamp
     */
    private final Map<String, Long> lastActiveTime = new ConcurrentHashMap<>();

    /**
     * 对话过期时间（30分钟）
     */
    private static final long CONVERSATION_TIMEOUT = 30 * 60 * 1000;

    /**
     * 最大历史消息数量
     */
    private static final int MAX_HISTORY_SIZE = 10;

    /**
     * 创建新会话
     */
    public String createConversation() {
        String conversationId = UUID.randomUUID().toString();
        conversations.put(conversationId, new ArrayList<>());
        lastActiveTime.put(conversationId, System.currentTimeMillis());
        log.info("Created new conversation: {}", conversationId);
        return conversationId;
    }

    /**
     * 添加系统消息（RAG prompt）
     */
    public void addSystemMessage(String conversationId, String content) {
        List<Message> history = getOrCreateConversation(conversationId);
        history.add(new SystemMessage(content));
        updateActiveTime(conversationId);
    }

    /**
     * 添加用户消息
     */
    public void addUserMessage(String conversationId, String content) {
        List<Message> history = getOrCreateConversation(conversationId);
        history.add(new UserMessage(content));
        updateActiveTime(conversationId);
    }

    /**
     * 添加 AI 回复
     */
    public void addAssistantMessage(String conversationId, String content) {
        List<Message> history = getOrCreateConversation(conversationId);
        history.add(new AssistantMessage(content));
        updateActiveTime(conversationId);

        // 保持历史消息数量在限制内（保留系统消息 + 最近的对话）
        if (history.size() > MAX_HISTORY_SIZE) {
            // 找到第一个非系统消息的位置
            int systemMsgCount = 0;
            for (Message msg : history) {
                if (msg instanceof SystemMessage) {
                    systemMsgCount++;
                } else {
                    break;
                }
            }

            // 保留系统消息 + 最近的对话
            List<Message> systemMessages = history.subList(0, systemMsgCount);
            List<Message> recentMessages = history.subList(
                    history.size() - (MAX_HISTORY_SIZE - systemMsgCount),
                    history.size());

            List<Message> newHistory = new ArrayList<>(systemMessages);
            newHistory.addAll(recentMessages);
            conversations.put(conversationId, newHistory);
        }
    }

    /**
     * 获取对话历史
     */
    public List<Message> getHistory(String conversationId) {
        cleanupExpiredConversations();
        return new ArrayList<>(conversations.getOrDefault(conversationId, new ArrayList<>()));
    }

    /**
     * 清除会话
     */
    public void clearConversation(String conversationId) {
        conversations.remove(conversationId);
        lastActiveTime.remove(conversationId);
        log.info("Cleared conversation: {}", conversationId);
    }

    /**
     * 获取或创建会话
     */
    private List<Message> getOrCreateConversation(String conversationId) {
        return conversations.computeIfAbsent(conversationId, k -> {
            lastActiveTime.put(k, System.currentTimeMillis());
            return new ArrayList<>();
        });
    }

    /**
     * 更新活跃时间
     */
    private void updateActiveTime(String conversationId) {
        lastActiveTime.put(conversationId, System.currentTimeMillis());
    }

    /**
     * 清理过期会话
     */
    private void cleanupExpiredConversations() {
        long now = System.currentTimeMillis();
        List<String> expiredIds = new ArrayList<>();

        for (Map.Entry<String, Long> entry : lastActiveTime.entrySet()) {
            if (now - entry.getValue() > CONVERSATION_TIMEOUT) {
                expiredIds.add(entry.getKey());
            }
        }

        for (String id : expiredIds) {
            conversations.remove(id);
            lastActiveTime.remove(id);
            log.info("Removed expired conversation: {}", id);
        }
    }

    /**
     * 获取活跃会话数量
     */
    public int getActiveConversationCount() {
        cleanupExpiredConversations();
        return conversations.size();
    }
}
