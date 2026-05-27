package com.liziye.spring.ai.lab.scenario.chat;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.routing.DefaultModelProviderManager;
import com.liziye.spring.ai.lab.scenario.chat.agent.SimpleChatAgent;
import com.liziye.spring.ai.lab.scenario.chat.controller.ChatController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Chat Agent 自动配置。
 *
 * <p>{@link TokenMetrics}、{@link LatencyMetrics}、{@link ConversationMemory} 等基础 Bean
 * 已由 {@link LabAutoConfiguration} (core) 提供，此处仅创建场景专属 Bean。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ChatAgentProperties.class)
public class ChatAgentAutoConfiguration {

    /**
     * 创建 {@link SimpleChatAgent} Bean。
     *
     * @param modelManager       模型提供者管理器
     * @param memory             对话记忆
     * @param tokenMetrics       Token 用量统计
     * @param latencyMetrics     延迟统计
     * @param chatAgentProperties Chat 场景配置属性
     * @return SimpleChatAgent 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public SimpleChatAgent simpleChatAgent(
            DefaultModelProviderManager modelManager,
            ConversationMemory memory,
            TokenMetrics tokenMetrics,
            LatencyMetrics latencyMetrics,
            ChatAgentProperties chatAgentProperties) {
        log.info("Creating SimpleChatAgent");
        return new SimpleChatAgent(modelManager, memory, List.of(),
                tokenMetrics, latencyMetrics, chatAgentProperties);
    }

    /**
     * 创建 {@link ChatController} Bean。
     *
     * @param agent SimpleChatAgent 实例
     * @return ChatController 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatController chatController(SimpleChatAgent agent) {
        return new ChatController(agent);
    }
}
