package com.liziye.spring.ai.lab.scenario.multiagent;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.routing.DefaultModelProviderManager;
import com.liziye.spring.ai.lab.scenario.multiagent.controller.MultiAgentController;
import com.liziye.spring.ai.lab.scenario.multiagent.orchestrator.MultiAgentOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 多 Agent 协作自动配置。
 *
 * <p>{@link TokenMetrics}、{@link LatencyMetrics}、{@link ConversationMemory}、
 * {@link DefaultModelProviderManager} 等基础 Bean 已由 {@link LabAutoConfiguration} (core) 统一提供。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(MultiAgentProperties.class)
public class MultiAgentAutoConfiguration {

    /**
     * 创建 {@link MultiAgentOrchestrator} Bean。
     *
     * @param modelManager  模型提供者管理器
     * @param memory        对话记忆
     * @param tokenMetrics  Token 用量统计
     * @param latencyMetrics 延迟统计
     * @param properties    多 Agent 配置属性
     * @return MultiAgentOrchestrator 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MultiAgentOrchestrator multiAgentOrchestrator(
            DefaultModelProviderManager modelManager,
            ConversationMemory memory,
            TokenMetrics tokenMetrics,
            LatencyMetrics latencyMetrics,
            MultiAgentProperties properties) {
        log.info("Creating MultiAgentOrchestrator: defaultMode={}", properties.getDefaultMode());
        return new MultiAgentOrchestrator(modelManager, memory, List.of(),
                tokenMetrics, latencyMetrics, properties);
    }

    /**
     * 创建 {@link MultiAgentController} Bean。
     *
     * @param orchestrator MultiAgentOrchestrator 实例
     * @return MultiAgentController 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MultiAgentController multiAgentController(MultiAgentOrchestrator orchestrator) {
        return new MultiAgentController(orchestrator);
    }
}
