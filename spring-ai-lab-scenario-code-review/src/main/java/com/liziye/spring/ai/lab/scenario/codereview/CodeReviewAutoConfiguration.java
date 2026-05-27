package com.liziye.spring.ai.lab.scenario.codereview;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.routing.DefaultModelProviderManager;
import com.liziye.spring.ai.lab.scenario.codereview.controller.CodeReviewController;
import com.liziye.spring.ai.lab.scenario.codereview.review.CodeReviewAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Code Review 自动配置。
 *
 * <p>{@link TokenMetrics}、{@link LatencyMetrics}、{@link ConversationMemory}、
 * {@link DefaultModelProviderManager} 等基础 Bean 已由 {@link LabAutoConfiguration} (core) 统一提供。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(CodeReviewProperties.class)
public class CodeReviewAutoConfiguration {

    /**
     * 创建 {@link CodeReviewAgent} Bean。
     *
     * @param modelManager     模型提供者管理器
     * @param memory           对话记忆
     * @param tokenMetrics     Token 用量统计
     * @param latencyMetrics    延迟统计
     * @param reviewProperties 代码审查配置属性
     * @return CodeReviewAgent 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public CodeReviewAgent codeReviewAgent(
            DefaultModelProviderManager modelManager,
            ConversationMemory memory,
            TokenMetrics tokenMetrics,
            LatencyMetrics latencyMetrics,
            CodeReviewProperties reviewProperties) {
        log.info("Creating CodeReviewAgent");
        return new CodeReviewAgent(modelManager, memory, List.of(),
                tokenMetrics, latencyMetrics, reviewProperties);
    }

    /**
     * 创建 {@link CodeReviewController} Bean。
     *
     * @param agent CodeReviewAgent 实例
     * @return CodeReviewController 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public CodeReviewController codeReviewController(CodeReviewAgent agent) {
        return new CodeReviewController(agent);
    }
}
