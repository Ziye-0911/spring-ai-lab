package com.liziye.spring.ai.lab.scenario.mcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP Server 配置属性。
 *
 * <p>配置前缀：{@code spring.ai.lab.mcp}。
 * 包含 Server 名称和版本、传输方式（sse/stdio）、SSE 端点路径、
 * 心跳配置以及启动时日志选项。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.lab.mcp")
public class McpProperties {

    /** MCP Server 名称 */
    private String serverName = "spring-ai-lab-mcp";

    /** MCP Server 版本 */
    private String version = "1.0.0";

    /** 传输方式：sse / stdio */
    private String transport = "sse";

    /** SSE 端点路径 */
    private String sseEndpoint = "/mcp/sse";

    /** SSE 消息端点路径 */
    private String sseMessageEndpoint = "/mcp/message";

    /** 是否启用心跳 */
    private boolean heartbeat = true;

    /** 心跳间隔（秒） */
    private int heartbeatIntervalSeconds = 30;

    /** 是否在启动时打印工具清单 */
    private boolean logToolsOnStartup = true;
}
