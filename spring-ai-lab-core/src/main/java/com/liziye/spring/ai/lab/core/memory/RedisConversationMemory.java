package com.liziye.spring.ai.lab.core.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liziye.spring.ai.lab.core.model.Message;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 对话记忆 Redis 实现。
 *
 * <p>使用 Redis List 存储对话历史，Redis String 存储 TTL 元数据。
 * 适合分布式部署和生产环境。
 *
 * <p>数据结构：
 * <ul>
 *   <li>{@code ailab:memory:{conversationId}:messages} → List（JSON 消息列表）</li>
 *   <li>{@code ailab:memory:{conversationId}:ttl} → String（过期时间戳）</li>
 *   <li>{@code ailab:memory:active} → Set（活跃会话 ID 集合）</li>
 * </ul>
 *
 * <p>使用条件：仅在 Redis 可用时通过 {@code @ConditionalOnClass} 加载。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class RedisConversationMemory implements ConversationMemory {

    private static final String KEY_PREFIX = "ailab:memory:";
    private static final String ACTIVE_SET_KEY = KEY_PREFIX + "active";

    private final RedisOperations redisOps;
    private final ObjectMapper objectMapper;
    private final long defaultTtlMillis;
    private final int maxMessageCount;

    public RedisConversationMemory(RedisOperations redisOps,
                                    long defaultTtlMillis,
                                    int maxMessageCount) {
        this.redisOps = redisOps;
        this.objectMapper = new ObjectMapper();
        this.defaultTtlMillis = defaultTtlMillis;
        this.maxMessageCount = maxMessageCount;
        log.info("RedisConversationMemory initialized: prefix={}, defaultTtl={}ms, maxMessages={}",
                KEY_PREFIX, defaultTtlMillis, maxMessageCount);
    }

    @Override
    public List<Message> getHistory(String conversationId, int maxMessages) {
        String key = messageKey(conversationId);

        try {
            // 检查是否过期
            if (isExpired(conversationId)) {
                clear(conversationId);
                return Collections.emptyList();
            }

            // 更新过期时间
            refreshTtl(conversationId);

            // 从 Redis List 获取消息（从旧到新）
            List<String> rawMessages = redisOps.lRange(key, 0, -1);
            if (rawMessages == null || rawMessages.isEmpty()) {
                return Collections.emptyList();
            }

            // 解析消息
            List<Message> messages = new ArrayList<>();
            int start = rawMessages.size() > maxMessages ? rawMessages.size() - maxMessages : 0;
            for (int i = start; i < rawMessages.size(); i++) {
                try {
                    messages.add(objectMapper.readValue(rawMessages.get(i), Message.class));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to deserialize message for conversation {}: {}", conversationId, e.getMessage());
                }
            }

            return messages;
        } catch (Exception e) {
            log.error("Failed to get history for conversation {}: {}", conversationId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void addMessage(String conversationId, Message message) {
        String key = messageKey(conversationId);

        try {
            // 更新过期
            refreshTtl(conversationId);

            // 序列化消息
            String json = objectMapper.writeValueAsString(message);

            // 添加到 Redis List（尾部追加）
            redisOps.rPush(key, json);

            // 限制消息数量（从头部裁剪）
            long currentSize = redisOps.lLen(key);
            if (currentSize > maxMessageCount) {
                redisOps.lTrim(key, currentSize - maxMessageCount, -1);
            }

            // 标记为活跃会话
            redisOps.sAdd(ACTIVE_SET_KEY, conversationId);

        } catch (Exception e) {
            log.error("Failed to add message for conversation {}: {}", conversationId, e.getMessage());
        }
    }

    @Override
    public void clear(String conversationId) {
        try {
            redisOps.delete(messageKey(conversationId));
            redisOps.delete(ttlKey(conversationId));
            redisOps.sRem(ACTIVE_SET_KEY, conversationId);
            log.debug("Conversation {} cleared from Redis", conversationId);
        } catch (Exception e) {
            log.error("Failed to clear conversation {}: {}", conversationId, e.getMessage());
        }
    }

    @Override
    public void expire(String conversationId, long ttl, TimeUnit unit) {
        try {
            long ttlMillis = unit.toMillis(ttl);
            long expireTime = System.currentTimeMillis() + ttlMillis;
            redisOps.set(ttlKey(conversationId), String.valueOf(expireTime));
            // 对消息 Key 也设置 Redis TTL
            redisOps.expire(messageKey(conversationId), Duration.ofMillis(ttlMillis));
            redisOps.expire(ttlKey(conversationId), Duration.ofMillis(ttlMillis));
        } catch (Exception e) {
            log.error("Failed to set expire for conversation {}: {}", conversationId, e.getMessage());
        }
    }

    @Override
    public long getTtl(String conversationId) {
        try {
            String ttlVal = redisOps.get(ttlKey(conversationId));
            if (ttlVal == null) {
                return -1;
            }
            long expireTime = Long.parseLong(ttlVal);
            long remaining = expireTime - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public List<String> listActiveConversations() {
        try {
            Set<String> activeIds = redisOps.sMembers(ACTIVE_SET_KEY);
            if (activeIds == null || activeIds.isEmpty()) {
                return Collections.emptyList();
            }
            // 过滤已过期的
            List<String> result = new ArrayList<>();
            for (String id : activeIds) {
                if (!isExpired(id)) {
                    result.add(id);
                } else {
                    // 懒清理
                    redisOps.sRem(ACTIVE_SET_KEY, id);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to list active conversations: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public int getMessageCount(String conversationId) {
        try {
            Long len = redisOps.lLen(messageKey(conversationId));
            return len != null ? len.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ===== 私有方法 =====

    private String messageKey(String conversationId) {
        return KEY_PREFIX + conversationId + ":messages";
    }

    private String ttlKey(String conversationId) {
        return KEY_PREFIX + conversationId + ":ttl";
    }

    private boolean isExpired(String conversationId) {
        try {
            String ttlVal = redisOps.get(ttlKey(conversationId));
            if (ttlVal == null) {
                return false; // 未设置 TTL
            }
            long expireTime = Long.parseLong(ttlVal);
            return System.currentTimeMillis() > expireTime;
        } catch (Exception e) {
            return false;
        }
    }

    private void refreshTtl(String conversationId) {
        try {
            long newExpireTime = System.currentTimeMillis() + defaultTtlMillis;
            redisOps.set(ttlKey(conversationId), String.valueOf(newExpireTime));
            redisOps.expire(messageKey(conversationId), Duration.ofMillis(defaultTtlMillis));
            redisOps.expire(ttlKey(conversationId), Duration.ofMillis(defaultTtlMillis));
        } catch (Exception e) {
            log.warn("Failed to refresh TTL for conversation {}: {}", conversationId, e.getMessage());
        }
    }

    /**
     * Redis 操作抽象接口。
     * 屏蔽底层实现差异（Lettuce / Jedis / StringRedisTemplate）。
     */
    public interface RedisOperations {

        // String 操作
        void set(String key, String value);
        String get(String key);

        // List 操作
        void rPush(String key, String value);
        List<String> lRange(String key, long start, long end);
        void lTrim(String key, long start, long end);
        Long lLen(String key);

        // Set 操作
        void sAdd(String key, String member);
        void sRem(String key, String member);
        Set<String> sMembers(String key);

        // 通用操作
        boolean expire(String key, Duration duration);
        boolean delete(String key);
    }
}
