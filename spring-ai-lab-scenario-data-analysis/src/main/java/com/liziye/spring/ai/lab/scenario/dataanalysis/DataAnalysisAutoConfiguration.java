package com.liziye.spring.ai.lab.scenario.dataanalysis;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.routing.DefaultModelProviderManager;
import com.liziye.spring.ai.lab.scenario.dataanalysis.controller.DataAnalysisController;
import com.liziye.spring.ai.lab.scenario.dataanalysis.orchestrator.DataAnalysisAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 数据分析自动配置。
 *
 * <p>自动装配数据分析场景所需的 Bean：
 * {@link com.liziye.spring.ai.lab.scenario.dataanalysis.orchestrator.DataAnalysisAgent} 编排器、
 * {@link com.liziye.spring.ai.lab.scenario.dataanalysis.controller.DataAnalysisController} 控制器。
 *
 * <p>注：{@code TokenMetrics}、{@code LatencyMetrics}、{@code ConversationMemory}、
 * {@code DefaultModelProviderManager} 等基础 Bean 已由
 * {@link com.liziye.spring.ai.lab.core.LabAutoConfiguration} (core) 统一提供。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(DataAnalysisProperties.class)
public class DataAnalysisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataAnalysisAgent dataAnalysisAgent(
            DefaultModelProviderManager modelManager,
            ConversationMemory memory,
            TokenMetrics tokenMetrics,
            LatencyMetrics latencyMetrics,
            DataAnalysisProperties properties) {
        log.info("Creating DataAnalysisAgent: datasourceType={}", properties.getDatasourceType());
        return new DataAnalysisAgent(modelManager, memory, List.of(),
                tokenMetrics, latencyMetrics, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataAnalysisController dataAnalysisController(DataAnalysisAgent agent) {
        return new DataAnalysisController(agent);
    }
}
