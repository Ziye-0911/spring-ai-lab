package com.liziye.spring.ai.lab.core.memory;

import com.liziye.spring.ai.lab.core.model.Message;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 对话记忆内存实现。
 *
 * <p>内部存储结构: {@code ConcurrentHashMap<String, SessionEntry>}，
 * {@link SessionEntry} 包含消息列表、创建时间、最后访问时间和 TTL。
 *
 * <p>过期清理机制：
 * <ul>
 *   <li>懒清理：每次 {@code getHistory/clear} 操作时检查当前会话是否过期</li>
 *   <li>定时清理：{@code @Scheduled} 每小时扫描过期会话</li>
 *   <li>默认 TTL：30 分钟</li>
 * </ul>
 *
 * <p>线程安全：{@code ConcurrentHashMap} + {@code synchronized} 块保护单会话写入。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class InMemoryConversationMemory implements ConversationMemory {

    private final ConcurrentHashMap<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private final long defaultTtlMillis;
    private final int maxMessageCount;

    public InMemoryConversationMemory(long defaultTtlMillis, int maxMessageCount) {
        this.defaultTtlMillis = defaultTtlMillis;
        this.maxMessageCount = maxMessageCount;
    }

    public InMemoryConversationMemory() {
        this(TimeUnit.MINUTES.toMillis(30), 20);
    }

    @Override
    public List<Message> getHistory(String conversationId, int maxMessages) {
        SessionEntry entry = sessions.get(conversationId);
        if (entry == null) {
            return Collections.emptyList();
        }
        // 懒清理: 检查是否过期
        if (entry.isExpired()) {
            sessions.remove(conversationId);
            log.debug("Conversation {} expired, cleared. TTL: {}ms", conversationId, entry.ttlMillis);
            return Collections.emptyList();
        }
        // 更新最后访问时间
        entry.touch();
        List<Message> history = entry.messages;
        if (history.size() <= maxMessages) {
            return new ArrayList<>(history);
        }
        return new ArrayList<>(history.subList(history.size() - maxMessages, history.size()));
    }

    @Override
    public void addMessage(String conversationId, Message message) {
        SessionEntry entry = sessions.computeIfAbsent(conversationId,
                k -> new SessionEntry(defaultTtlMillis, maxMessageCount));
        entry.touch();
        synchronized (entry) {
            entry.messages.add(message);
        }
    }

    @Override
    public void clear(String conversationId) {
        sessions.remove(conversationId);
        log.debug("Conversation {} cleared", conversationId);
    }

    @Override
    public void expire(String conversationId, long ttl, TimeUnit unit) {
        SessionEntry entry = sessions.get(conversationId);
        if (entry != null) {
            entry.touch();
            entry.ttlMillis = unit.toMillis(ttl);
            log.debug("Conversation {} TTL set to {}ms", conversationId, entry.ttlMillis);
        }
    }

    @Override
    public long getTtl(String conversationId) {
        SessionEntry entry = sessions.get(conversationId);
        if (entry == null) {
            return -1;
        }
        if (entry.ttlMillis <= 0) {
            return -1;
        }
        long elapsed = System.currentTimeMillis() - entry.lastAccessTime;
        long remaining = entry.ttlMillis - elapsed;
        return Math.max(remaining, 0);
    }

    @Override
    public List<String> listActiveConversations() {
        return sessions.entrySet().stream()
                .filter(e -> !e.getValue().isExpired())
                .map(e -> e.getKey())
                .collect(Collectors.toList());
    }

    @Override
    public int getMessageCount(String conversationId) {
        SessionEntry entry = sessions.get(conversationId);
        if (entry == null) {
            return 0;
        }
        // 检查过期
        if (entry.isExpired()) {
            sessions.remove(conversationId);
            return 0;
        }
        entry.touch();
        return entry.messages.size();
    }

    /**
     * 定时清理过期会话。由 Spring @Scheduled 触发。
     */
    public void scheduledCleanup() {
        int removed = 0;
        for (String id : sessions.keySet()) {
            SessionEntry entry = sessions.get(id);
            if (entry != null && entry.isExpired()) {
                sessions.remove(id);
                removed++;
            }
        }
        if (removed > 0) {
            log.info("Scheduled cleanup: removed {} expired conversations", removed);
        }
    }

    /**
     * 会话存储单元。
     */
    private static class SessionEntry {
        final List<Message> messages = Collections.synchronizedList(new ArrayList<>());
        final Instant createdAt;
        volatile long lastAccessTime;
        volatile long ttlMillis;
        final int maxCount;

        SessionEntry(long ttlMillis, int maxCount) {
            this.ttlMillis = ttlMillis;
            this.maxCount = maxCount;
            this.createdAt = Instant.now();
            this.lastAccessTime = System.currentTimeMillis();
        }

        void touch() {
            this.lastAccessTime = System.currentTimeMillis();
            // 消息数超限时，移除最早的消息
            while (messages.size() > maxCount) {
                messages.remove(0);
            }
        }

        boolean isExpired() {
            if (ttlMillis <= 0) {
                return false; // 永不过期
            }
            return System.currentTimeMillis() - lastAccessTime > ttlMillis;
        }
    }
}
