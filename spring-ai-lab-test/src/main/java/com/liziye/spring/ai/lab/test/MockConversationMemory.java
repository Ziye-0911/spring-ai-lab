package com.liziye.spring.ai.lab.test;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.model.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Mock 对话记忆 — 简单的内存存储，用于单元测试。
 */
public class MockConversationMemory implements ConversationMemory {

    private final Map<String, List<Message>> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> ttls = new ConcurrentHashMap<>();

    @Override
    public List<Message> getHistory(String conversationId, int maxMessages) {
        List<Message> history = sessions.get(conversationId);
        if (history == null) {
            return Collections.emptyList();
        }
        if (history.size() <= maxMessages) {
            return new ArrayList<>(history);
        }
        return new ArrayList<>(history.subList(history.size() - maxMessages, history.size()));
    }

    @Override
    public void addMessage(String conversationId, Message message) {
        sessions.computeIfAbsent(conversationId, k -> new ArrayList<>()).add(message);
    }

    @Override
    public void clear(String conversationId) {
        sessions.remove(conversationId);
        ttls.remove(conversationId);
    }

    @Override
    public void expire(String conversationId, long ttl, TimeUnit unit) {
        ttls.put(conversationId, unit.toMillis(ttl));
    }

    @Override
    public long getTtl(String conversationId) {
        return ttls.getOrDefault(conversationId, -1L);
    }

    @Override
    public List<String> listActiveConversations() {
        return new ArrayList<>(sessions.keySet());
    }

    @Override
    public int getMessageCount(String conversationId) {
        List<Message> history = sessions.get(conversationId);
        return history != null ? history.size() : 0;
    }

    public void clearAll() {
        sessions.clear();
        ttls.clear();
    }
}
