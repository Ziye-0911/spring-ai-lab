package com.liziye.spring.ai.lab.scenario.mcp;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 MCP Server 场景。
 *
 * <p>在 Spring Boot 应用启动类添加此注解，自动装配 MCP Server，
 * 将 AI 工具和资源通过 Model Context Protocol 暴露给 MCP 客户端（如 Claude Desktop、Cursor 等）。
 *
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableMcp
 * public class MyApp {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApp.class, args);
 *     }
 * }
 * </pre>
 *
 * <p>配置示例：
 * <pre>
 * spring:
 *   ai:
 *     lab:
 *       mcp:
 *         server-name: my-ai-server
 *         version: 1.0.0
 *         transport: sse           # sse / stdio
 *         sse-endpoint: /mcp/sse
 *         sse-message-endpoint: /mcp/message
 * </pre>
 *
 * @author liziye
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(McpAutoConfiguration.class)
public @interface EnableMcp {

    /** MCP Server 名称 */
    String serverName() default "spring-ai-lab-mcp";

    /** MCP Server 版本 */
    String version() default "1.0.0";

    /** 传输方式：sse / stdio */
    String transport() default "sse";
}
