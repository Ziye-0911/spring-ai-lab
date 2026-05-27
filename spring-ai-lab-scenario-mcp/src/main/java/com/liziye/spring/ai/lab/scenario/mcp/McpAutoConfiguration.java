package com.liziye.spring.ai.lab.scenario.mcp;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.routing.DefaultModelProviderManager;
import com.liziye.spring.ai.lab.scenario.mcp.controller.McpSseController;
import com.liziye.spring.ai.lab.scenario.mcp.protocol.McpJsonRpcHandler;
import com.liziye.spring.ai.lab.scenario.mcp.protocol.McpToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MCP Server 自动配置。
 *
 * <p>自动装配 MCP Server 所需的 Bean：
 * {@link com.liziye.spring.ai.lab.scenario.mcp.protocol.McpToolRegistry} 工具注册中心、
 * {@link com.liziye.spring.ai.lab.scenario.mcp.protocol.McpJsonRpcHandler} JSON-RPC 协议处理器、
 * {@link com.liziye.spring.ai.lab.scenario.mcp.controller.McpSseController} SSE 传输层控制器。
 *
 * <p>当 {@code spring.ai.lab.mcp.transport} 配置为 {@code sse} 时（默认），
 * 自动启用 SSE 传输方式。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(McpProperties.class)
public class McpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public McpToolRegistry mcpToolRegistry() {
        McpToolRegistry registry = new McpToolRegistry();
        log.info("McpToolRegistry initialized");
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public McpJsonRpcHandler mcpJsonRpcHandler(McpToolRegistry toolRegistry,
                                                McpProperties properties,
                                                DefaultModelProviderManager modelManager,
                                                ConversationMemory memory,
                                                TokenMetrics tokenMetrics) {
        return new McpJsonRpcHandler(toolRegistry, properties, modelManager, memory, tokenMetrics);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.ai.lab.mcp", name = "transport", havingValue = "sse",
            matchIfMissing = true)
    public McpSseController mcpSseController(McpJsonRpcHandler handler, McpProperties properties) {
        log.info("MCP SSE transport enabled: endpoint={}", properties.getSseEndpoint());
        return new McpSseController(handler, properties);
    }
}
