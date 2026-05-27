package com.liziye.spring.ai.lab.core.routing;

import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

/**
 * 模型提供商管理器 — 管理多模型实例，支持动态切换和降级。
 *
 * <p>职责：
 * <ol>
 *   <li>管理多个模型提供商实例（OpenAI、Ollama、通义千问等）</li>
 *   <li>支持按名称动态获取 {@link ChatClient}</li>
 *   <li>支持模型分组（default / fallback / cheap）</li>
 *   <li>支持模型健康检查</li>
 * </ol>
 *
 * @author liziye
 * @since 1.0.0
 */
public interface ModelProviderManager {

    /**
     * 根据模型名称获取 ChatClient。
     *
     * @param modelName 模型名称（如 "openai"、"ollama"、"qwen"）
     * @return ChatClient 实例
     */
    ChatClient getChatClient(String modelName);

    /**
     * 获取默认模型。
     *
     * @return 默认 ChatClient
     */
    ChatClient getDefaultChatClient();

    /**
     * 获取降级模型（主模型不可用时使用）。
     *
     * @return 降级 ChatClient
     */
    ChatClient getFallbackChatClient();

    /**
     * 获取指定模型分组的 ChatClient。
     *
     * @param groupName 分组名称（如 "cheap"、"premium"）
     * @return 对应分组的 ChatClient
     */
    ChatClient getChatClientByGroup(String groupName);

    /**
     * 获取所有可用的模型名称列表。
     *
     * @return 可用模型名称列表
     */
    List<String> getAvailableModels();

    /**
     * 检查指定模型是否可用（健康检查）。
     *
     * @param modelName 模型名称
     * @return true 表示可用
     */
    boolean isAvailable(String modelName);

    /**
     * 注册模型提供商。
     *
     * @param name       模型名称
     * @param chatClient ChatClient 实例
     */
    void registerProvider(String name, ChatClient chatClient);
}
