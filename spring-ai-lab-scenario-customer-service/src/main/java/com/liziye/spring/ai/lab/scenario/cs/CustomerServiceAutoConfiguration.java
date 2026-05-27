package com.liziye.spring.ai.lab.scenario.cs;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.routing.DefaultModelProviderManager;
import com.liziye.spring.ai.lab.scenario.cs.controller.CustomerServiceController;
import com.liziye.spring.ai.lab.scenario.cs.intent.IntentClassifier;
import com.liziye.spring.ai.lab.scenario.cs.orchestrator.CustomerServiceOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 智能客服自动配置。
 *
 * <p>自动装配智能客服场景所需的 Bean：
 * {@link com.liziye.spring.ai.lab.scenario.cs.intent.IntentClassifier} 意图识别器、
 * {@link com.liziye.spring.ai.lab.scenario.cs.orchestrator.CustomerServiceOrchestrator} 编排器、
 * {@link com.liziye.spring.ai.lab.scenario.cs.controller.CustomerServiceController} 控制器。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(CustomerServiceProperties.class)
public class CustomerServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IntentClassifier intentClassifier(CustomerServiceProperties properties) {
        log.info("IntentClassifier initialized: categories={}", properties.getIntent().getCategories());
        return new IntentClassifier(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomerServiceOrchestrator customerServiceOrchestrator(
            DefaultModelProviderManager modelManager,
            ConversationMemory memory,
            IntentClassifier intentClassifier,
            TokenMetrics tokenMetrics,
            LatencyMetrics latencyMetrics,
            CustomerServiceProperties properties) {
        log.info("CustomerServiceOrchestrator initialized: agentName={}", properties.getAgentName());
        return new CustomerServiceOrchestrator(
                modelManager, memory, intentClassifier,
                tokenMetrics, latencyMetrics, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomerServiceController customerServiceController(
            CustomerServiceOrchestrator orchestrator) {
        return new CustomerServiceController(orchestrator);
    }
}
