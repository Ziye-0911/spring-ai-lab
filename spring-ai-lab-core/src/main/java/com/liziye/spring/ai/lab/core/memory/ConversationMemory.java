package com.liziye.spring.ai.lab.core.memory;

import com.liziye.spring.ai.lab.core.model.Message;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 对话记忆管理接口。
 *
 * <p>职责：管理多轮对话的上下文，让 AI 记住之前的对话内容。
 *
 * <p>支持两种实现：
 * <ul>
 *   <li>{@link InMemoryConversationMemory}：基于 {@code ConcurrentHashMap}，适合单机开发和测试</li>
 *   <li>{@link RedisConversationMemory}：基于 Redis，适合分布式部署和生产环境</li>
 * </ul>
 *
 * @author liziye
 * @since 1.0.0
 */
public interface ConversationMemory {

    /**
     * 获取指定会话的历史消息列表。
     *
     * @param conversationId 会话唯一标识
     * @param maxMessages    最大返回消息条数（防止上下文过长）
     * @return 历史消息列表，按时间升序
     */
    List<Message> getHistory(String conversationId, int maxMessages);

    /**
     * 向指定会话追加一条新消息。
     *
     * @param conversationId 会话唯一标识
     * @param message        要追加的消息（包含 role 和 content）
     */
    void addMessage(String conversationId, Message message);

    /**
     * 清空指定会话的全部历史记录。
     *
     * @param conversationId 会话唯一标识
     */
    void clear(String conversationId);

    /**
     * 设置会话过期时间（距离最后一次操作）。
     *
     * @param conversationId 会话ID
     * @param ttl            过期时长
     * @param unit           时间单位
     */
    void expire(String conversationId, long ttl, TimeUnit unit);

    /**
     * 获取会话剩余有效时间。
     *
     * @param conversationId 会话ID
     * @return 剩余毫秒数，-1 表示永不过期
     */
    long getTtl(String conversationId);

    /**
     * 获取当前所有活跃（未过期）的会话ID列表。
     *
     * @return 活跃会话ID列表
     */
    List<String> listActiveConversations();

    /**
     * 获取指定会话的消息总数。
     *
     * @param conversationId 会话唯一标识
     * @return 消息总数
     */
    int getMessageCount(String conversationId);
}
