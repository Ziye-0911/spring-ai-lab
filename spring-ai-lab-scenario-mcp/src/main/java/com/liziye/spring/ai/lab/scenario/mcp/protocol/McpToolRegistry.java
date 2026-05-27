package com.liziye.spring.ai.lab.scenario.mcp.protocol;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * MCP 工具注册中心。
 *
 * <p>管理 MCP Server 对外暴露的工具。
 *
 * <p>支持两种注册方式：
 * <ul>
 *   <li>声明式：通过 {@link #registerTool(McpToolDefinition)} 注册工具定义和处理器</li>
 *   <li>Lambda 式：通过 {@link #register(String, String, Map, Function)} 快速注册</li>
 * </ul>
 *
 * <p>内置工具：
 * <ul>
 *   <li>{@code echo} — 回显消息</li>
 *   <li>{@code get_time} — 获取当前时间</li>
 *   <li>{@code calculate} — 简单数学计算</li>
 * </ul>
 *
 * <p>包含内部类 {@link McpToolDefinition}、{@link McpResource}、
 * {@link McpPrompt} 用于定义工具、资源和 Prompt 模板。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class McpToolRegistry {

    private final Map<String, ToolEntry> tools = new ConcurrentHashMap<>();

    /**
     * 注册内置工具。
     */
    public McpToolRegistry() {
        registerBuiltinTools();
    }

    private void registerBuiltinTools() {
        // echo 工具
        register("echo", "Echo back the input message",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "message", Map.of("type", "string", "description", "The message to echo")
                        ),
                        "required", List.of("message")
                ),
                args -> "[ECHO] " + args.getOrDefault("message", ""));

        // get_time 工具
        register("get_time", "Get the current server time",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "format", Map.of("type", "string",
                                        "description", "Time format: full / iso / timestamp",
                                        "enum", List.of("full", "iso", "timestamp"))
                        )
                ),
                args -> {
                    String format = (String) args.getOrDefault("format", "full");
                    return switch (format) {
                        case "iso" -> java.time.Instant.now().toString();
                        case "timestamp" -> String.valueOf(System.currentTimeMillis());
                        default -> new java.util.Date().toString();
                    };
                });

        // calculate 工具
        register("calculate", "Perform a simple mathematical calculation (add, subtract, multiply, divide)",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "operation", Map.of("type", "string",
                                        "description", "The operation to perform",
                                        "enum", List.of("add", "subtract", "multiply", "divide")),
                                "a", Map.of("type", "number", "description", "First number"),
                                "b", Map.of("type", "number", "description", "Second number")
                        ),
                        "required", List.of("operation", "a", "b")
                ),
                args -> {
                    String op = (String) args.get("operation");
                    double a = ((Number) args.get("a")).doubleValue();
                    double b = ((Number) args.get("b")).doubleValue();
                    double result = switch (op) {
                        case "add" -> a + b;
                        case "subtract" -> a - b;
                        case "multiply" -> a * b;
                        case "divide" -> b != 0 ? a / b : Double.NaN;
                        default -> throw new IllegalArgumentException("Unknown operation: " + op);
                    };
                    return String.format("%.4f", result);
                });

        log.info("Registered {} built-in MCP tools: echo, get_time, calculate", 3);
    }

    /**
     * Lambda 式注册工具。
     *
     * @param name        工具名称
     * @param description 工具描述
     * @param inputSchema JSON Schema 格式的输入参数定义
     * @param handler     工具执行处理器
     */
    public void register(String name, String description,
                          Map<String, Object> inputSchema,
                          Function<Map<String, Object>, String> handler) {
        registerTool(new McpToolDefinition(name, description, inputSchema, handler));
    }

    /**
     * 声明式注册工具。
     *
     * @param tool 工具定义
     */
    public void registerTool(McpToolDefinition tool) {
        tools.put(tool.getName(), new ToolEntry(tool));
        log.info("[MCP-TOOL] Registered tool: name={}", tool.getName());
    }

    /**
     * 注销工具。
     *
     * @param name 工具名称
     */
    public void unregister(String name) {
        tools.remove(name);
        log.info("[MCP-TOOL] Unregistered tool: name={}", name);
    }

    /**
     * 列出所有工具定义。
     *
     * @return 已注册的工具定义列表
     */
    public List<McpToolDefinition> listTools() {
        List<McpToolDefinition> result = new ArrayList<>();
        for (ToolEntry entry : tools.values()) {
            result.add(entry.definition);
        }
        return result;
    }

    /**
     * 调用工具。
     *
     * @param name      工具名称
     * @param arguments 工具参数
     * @return 工具执行结果
     * @throws IllegalArgumentException 如果工具不存在
     */
    public String callTool(String name, Map<String, Object> arguments) {
        ToolEntry entry = tools.get(name);
        if (entry == null) {
            throw new IllegalArgumentException("Tool not found: " + name);
        }
        return entry.definition.call(arguments);
    }

    /**
     * 获取工具数量。
     *
     * @return 已注册的工具总数
     */
    public int getToolCount() {
        return tools.size();
    }

    // ===== 内部类 =====

    private static class ToolEntry {
        final McpToolDefinition definition;

        ToolEntry(McpToolDefinition definition) {
            this.definition = definition;
        }
    }

    /**
     * MCP 工具定义。
     */
    public static class McpToolDefinition {
        private final String name;
        private final String description;
        private final Map<String, Object> inputSchema;
        private final Function<Map<String, Object>, String> handler;

        public McpToolDefinition(String name, String description,
                                  Map<String, Object> inputSchema,
                                  Function<Map<String, Object>, String> handler) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
            this.handler = handler;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public Map<String, Object> getInputSchema() { return inputSchema; }

        public String call(Map<String, Object> arguments) {
            return handler.apply(arguments);
        }
    }

    /**
     * MCP 资源定义。
     */
    public static class McpResource {
        private final String uri;
        private final String name;
        private final String description;
        private final String mimeType;
        private final String content;

        public McpResource(String uri, String name, String description,
                            String mimeType, String content) {
            this.uri = uri;
            this.name = name;
            this.description = description;
            this.mimeType = mimeType;
            this.content = content;
        }

        public String getUri() { return uri; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getMimeType() { return mimeType; }
        public String getContent() { return content; }
    }

    /**
     * MCP Prompt 定义。
     */
    public static class McpPrompt {
        private final String name;
        private final String description;
        private final List<Map<String, Object>> arguments;
        private final String template;

        public McpPrompt(String name, String description,
                          List<Map<String, Object>> arguments, String template) {
            this.name = name;
            this.description = description;
            this.arguments = arguments;
            this.template = template;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<Map<String, Object>> getArguments() { return arguments; }

        /**
         * 渲染 Prompt 模板，替换参数占位符。
         */
        public String render(Map<String, Object> args) {
            String result = template;
            for (Map.Entry<String, Object> entry : args.entrySet()) {
                result = result.replace("{{" + entry.getKey() + "}}",
                        String.valueOf(entry.getValue()));
            }
            return result;
        }
    }
}
