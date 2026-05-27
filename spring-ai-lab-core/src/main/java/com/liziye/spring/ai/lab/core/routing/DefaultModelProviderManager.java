package com.liziye.spring.ai.lab.core.routing;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认模型提供商管理器实现。
 *
 * <p>基于 {@code ConcurrentHashMap} 管理多个模型提供商实例，
 * 支持按名称、分组动态获取 {@link ChatClient}，以及默认/降级模型切换。
 *
 * @author liziye
 * @since 1.0.0
 */
public class DefaultModelProviderManager implements ModelProviderManager {

    private final Map<String, ChatClient> clients = new ConcurrentHashMap<>();
    private final Map<String, ChatModel> models = new ConcurrentHashMap<>();
    private final Map<String, String> groups = new ConcurrentHashMap<>();
    private String defaultName;
    private String fallbackName;

    /**
     * 设置默认模型名称。
     *
     * @param name 模型名称
     */
    public void setDefaultModel(String name) {
        this.defaultName = name;
    }

    /**
     * 设置降级模型名称。
     *
     * @param name 模型名称
     */
    public void setFallbackModel(String name) {
        this.fallbackName = name;
    }

    /**
     * 注册模型（通过 {@link ChatClient}）。
     *
     * @param name       模型名称
     * @param chatClient ChatClient 实例
     * @param groupNames 所属分组名称列表
     */
    public void addModel(String name, ChatClient chatClient, String... groupNames) {
        clients.put(name, chatClient);
        for (String group : groupNames) {
            groups.put(group, name);
        }
    }

    /**
     * 注册模型（通过 {@link ChatModel}）。
     *
     * @param name       模型名称
     * @param chatModel  ChatModel 实例
     * @param groupNames 所属分组名称列表
     */
    public void addModel(String name, ChatModel chatModel, String... groupNames) {
        ChatClient client = ChatClient.builder(chatModel).build();
        addModel(name, client, groupNames);
        models.put(name, chatModel);
    }

    /**
     * 获取默认 ChatModel（用于直接调用模型）。
     *
     * @return 默认 ChatModel
     * @throws IllegalStateException 当没有注册任何模型时抛出
     */
    public ChatModel getDefaultModel() {
        if (defaultName != null && models.containsKey(defaultName)) {
            return models.get(defaultName);
        }
        if (!models.isEmpty()) {
            return models.values().iterator().next();
        }
        throw new IllegalStateException("No ChatModel registered");
    }

    @Override
    public ChatClient getChatClient(String modelName) {
        ChatClient client = clients.get(modelName);
        if (client == null) {
            throw new IllegalArgumentException("Model not found: " + modelName);
        }
        return client;
    }

    @Override
    public ChatClient getDefaultChatClient() {
        if (defaultName != null && clients.containsKey(defaultName)) {
            return clients.get(defaultName);
        }
        if (!clients.isEmpty()) {
            return clients.values().iterator().next();
        }
        throw new IllegalStateException("No ChatClient registered");
    }

    @Override
    public ChatClient getFallbackChatClient() {
        if (fallbackName != null && clients.containsKey(fallbackName)) {
            return clients.get(fallbackName);
        }
        return getDefaultChatClient();
    }

    @Override
    public ChatClient getChatClientByGroup(String groupName) {
        String modelName = groups.get(groupName);
        if (modelName != null) {
            return getChatClient(modelName);
        }
        return getDefaultChatClient();
    }

    @Override
    public List<String> getAvailableModels() {
        return new ArrayList<>(clients.keySet());
    }

    @Override
    public boolean isAvailable(String modelName) {
        return clients.containsKey(modelName);
    }

    @Override
    public void registerProvider(String name, ChatClient chatClient) {
        clients.put(name, chatClient);
    }
}
