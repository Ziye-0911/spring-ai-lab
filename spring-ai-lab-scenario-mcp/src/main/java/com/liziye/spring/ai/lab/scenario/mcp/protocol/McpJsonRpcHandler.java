package com.liziye.spring.ai.lab.scenario.mcp.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.model.Message;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.routing.DefaultModelProviderManager;
import com.liziye.spring.ai.lab.scenario.mcp.McpProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * MCP JSON-RPC 协议处理器。
 *
 * <p>实现 MCP 协议核心方法：{@code initialize}、{@code tools/list}、
 * {@code tools/call}、{@code resources/list}、{@code resources/read}、
 * {@code prompts/list}、{@code prompts/get}。
 *
 * <p>遵循 MCP 规范（Model Context Protocol 2024-11-05），支持：
 * <ul>
 *   <li>客户端能力协商</li>
 *   <li>工具发现与调用</li>
 *   <li>资源注册与读取（会话记忆、模型信息）</li>
 *   <li>Prompt 模板发现与渲染</li>
 * </ul>
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class McpJsonRpcHandler {

    private final McpToolRegistry toolRegistry;
    private final McpProperties properties;
    private final DefaultModelProviderManager modelManager;
    private final ConversationMemory memory;
    private final TokenMetrics tokenMetrics;
    private final ObjectMapper objectMapper;

    /** 会话 ID → 已注册的 Resource 及 Prompt */
    private final Map<String, List<McpToolRegistry.McpResource>> sessionResources = new ConcurrentHashMap<>();
    private final Map<String, List<McpToolRegistry.McpPrompt>> sessionPrompts = new ConcurrentHashMap<>();

    /** 客户端能力声明 */
    private final Map<String, Map<String, Object>> clientCapabilities = new ConcurrentHashMap<>();

    public McpJsonRpcHandler(McpToolRegistry toolRegistry,
                              McpProperties properties,
                              DefaultModelProviderManager modelManager,
                              ConversationMemory memory,
                              TokenMetrics tokenMetrics) {
        this.toolRegistry = toolRegistry;
        this.properties = properties;
        this.modelManager = modelManager;
        this.memory = memory;
        this.tokenMetrics = tokenMetrics;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 处理 JSON-RPC 请求，返回 JSON-RPC 响应。
     *
     * <p>根据方法名路由到对应的处理器：{@code initialize}、{@code tools/list}、
     * {@code tools/call}、{@code resources/list}、{@code resources/read}、
     * {@code prompts/list}、{@code prompts/get}、{@code ping}。
     *
     * @param sessionId 会话 ID
     * @param request   JSON-RPC 请求体，需包含 {@code method} 字段
     * @return JSON-RPC 响应，通知类方法返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> handleRequest(String sessionId, Map<String, Object> request) {
        String method = (String) request.get("method");
        Object id = request.getOrDefault("id", null);
        Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", Collections.emptyMap());

        try {
            Object result = switch (method) {
                case "initialize" -> handleInitialize(sessionId, params);
                case "notifications/initialized" -> handleInitialized(sessionId);
                case "tools/list" -> handleToolsList(sessionId);
                case "tools/call" -> handleToolsCall(sessionId, params);
                case "resources/list" -> handleResourcesList(sessionId);
                case "resources/read" -> handleResourcesRead(sessionId, params);
                case "prompts/list" -> handlePromptsList(sessionId);
                case "prompts/get" -> handlePromptsGet(sessionId, params);
                case "ping" -> handlePing();
                default -> throw new IllegalArgumentException("Unknown method: " + method);
            };

            if (result == null) {
                // 通知类方法无响应
                return null;
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", id);
            response.put("result", result);
            return response;

        } catch (Exception e) {
            log.error("[MCP] method={} session={} error={}", method, sessionId, e.getMessage());
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("jsonrpc", "2.0");
            error.put("id", id);
            Map<String, Object> errorObj = new LinkedHashMap<>();
            errorObj.put("code", -32000);
            errorObj.put("message", e.getMessage());
            error.put("error", errorObj);
            return error;
        }
    }

    // ===== initialize =====

    private Map<String, Object> handleInitialize(String sessionId, Map<String, Object> params) {
        log.info("[MCP] initialize: session={} clientName={} clientVersion={}",
                sessionId, params.get("clientName"), params.get("clientVersion"));

        // 记录客户端能力
        if (params.containsKey("capabilities")) {
            clientCapabilities.put(sessionId, (Map<String, Object>) params.get("capabilities"));
        }

        Map<String, Object> capabilities = Map.of(
                "tools", Map.of("listChanged", true),
                "resources", Map.of("subscribe", true, "listChanged", true),
                "prompts", Map.of("listChanged", true),
                "logging", Map.of()
        );

        Map<String, Object> serverInfo = Map.of(
                "name", properties.getServerName(),
                "version", properties.getVersion()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", capabilities);
        result.put("serverInfo", serverInfo);
        result.put("instructions",
                "Spring AI Lab MCP Server. Use tools/list to discover available tools, " +
                "resources/list to browse resources, prompts/list to get prompt templates.");

        return result;
    }

    private Object handleInitialized(String sessionId) {
        log.info("[MCP] initialized: session={}", sessionId);
        // 通知类方法，不需要返回结果
        return Collections.emptyMap();
    }

    // ===== tools =====

    private Map<String, Object> handleToolsList(String sessionId) {
        List<McpToolRegistry.McpToolDefinition> tools = toolRegistry.listTools();
        List<Map<String, Object>> toolList = new ArrayList<>();
        for (McpToolRegistry.McpToolDefinition tool : tools) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("name", tool.getName());
            t.put("description", tool.getDescription());
            t.put("inputSchema", tool.getInputSchema());
            toolList.add(t);
        }
        log.debug("[MCP] tools/list: session={} count={}", sessionId, toolList.size());
        return Map.of("tools", toolList);
    }

    private Map<String, Object> handleToolsCall(String sessionId, Map<String, Object> params) {
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Collections.emptyMap());

        log.info("[MCP] tools/call: session={} tool={}", sessionId, toolName);

        try {
            String result = toolRegistry.callTool(toolName, arguments);
            List<Map<String, Object>> content = new ArrayList<>();
            Map<String, Object> textContent = new LinkedHashMap<>();
            textContent.put("type", "text");
            textContent.put("text", result);
            content.add(textContent);
            return Map.of("content", content);
        } catch (Exception e) {
            Map<String, Object> errorContent = new LinkedHashMap<>();
            errorContent.put("type", "text");
            errorContent.put("text", "Tool execution failed: " + e.getMessage());
            return Map.of("content", List.of(errorContent), "isError", true);
        }
    }

    // ===== resources =====

    private Map<String, Object> handleResourcesList(String sessionId) {
        List<McpToolRegistry.McpResource> resources = sessionResources.getOrDefault(sessionId, Collections.emptyList());

        // 默认添加系统资源
        List<Map<String, Object>> resourceList = new ArrayList<>(resources.size() + 2);

        // 会话记忆资源
        Map<String, Object> memResource = new LinkedHashMap<>();
        memResource.put("uri", "memory://" + sessionId + "/history");
        memResource.put("name", "Conversation History");
        memResource.put("description", "Current session conversation history");
        memResource.put("mimeType", "application/json");
        resourceList.add(memResource);

        // 模型信息
        Map<String, Object> modelResource = new LinkedHashMap<>();
        modelResource.put("uri", "config://models");
        modelResource.put("name", "Available Models");
        modelResource.put("description", "List of available AI models and their capabilities");
        modelResource.put("mimeType", "application/json");
        resourceList.add(modelResource);

        for (McpToolRegistry.McpResource r : resources) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("uri", r.getUri());
            res.put("name", r.getName());
            res.put("description", r.getDescription());
            res.put("mimeType", r.getMimeType() != null ? r.getMimeType() : "text/plain");
            resourceList.add(res);
        }

        return Map.of("resources", resourceList);
    }

    private Map<String, Object> handleResourcesRead(String sessionId, Map<String, Object> params) {
        String uri = (String) params.get("uri");

        if (uri.startsWith("memory://")) {
            List<Message> history = memory.getHistory(sessionId, 50);
            List<Map<String, Object>> content = new ArrayList<>();
            for (Message msg : history) {
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("type", "text");
                c.put("text", "[" + msg.getRole() + "] " + msg.getContent());
                content.add(c);
            }
            return Map.of("contents", List.of(Map.of("uri", uri, "mimeType", "application/json",
                    "text", content.toString())));
        }

        if (uri.startsWith("config://models")) {
            List<String> models = modelManager.getAvailableModels();
            Map<String, Object> textContent = new LinkedHashMap<>();
            textContent.put("type", "text");
            textContent.put("text", String.join("\n", models));
            return Map.of("contents", List.of(Map.of("uri", uri, "mimeType", "application/json",
                    "text", textContent.get("text"))));
        }

        // 自定义资源
        List<McpToolRegistry.McpResource> resources = sessionResources.getOrDefault(sessionId, Collections.emptyList());
        for (McpToolRegistry.McpResource r : resources) {
            if (r.getUri().equals(uri)) {
                Map<String, Object> textContent = new LinkedHashMap<>();
                textContent.put("type", "text");
                textContent.put("text", r.getContent());
                return Map.of("contents", List.of(Map.of("uri", uri,
                        "mimeType", r.getMimeType() != null ? r.getMimeType() : "text/plain",
                        "text", r.getContent())));
            }
        }

        throw new IllegalArgumentException("Resource not found: " + uri);
    }

    // ===== prompts =====

    private Map<String, Object> handlePromptsList(String sessionId) {
        List<McpToolRegistry.McpPrompt> prompts = sessionPrompts.getOrDefault(sessionId, Collections.emptyList());

        List<Map<String, Object>> promptList = new ArrayList<>(prompts.size() + 1);

        // 默认 Prompt
        Map<String, Object> defaultPrompt = new LinkedHashMap<>();
        defaultPrompt.put("name", "chat");
        defaultPrompt.put("description", "Simple chat prompt template");
        Map<String, Object> arg = new LinkedHashMap<>();
        arg.put("name", "message");
        arg.put("description", "The user message");
        arg.put("required", true);
        defaultPrompt.put("arguments", List.of(arg));
        promptList.add(defaultPrompt);

        for (McpToolRegistry.McpPrompt p : prompts) {
            Map<String, Object> pr = new LinkedHashMap<>();
            pr.put("name", p.getName());
            pr.put("description", p.getDescription());
            if (p.getArguments() != null) {
                pr.put("arguments", p.getArguments());
            }
            promptList.add(pr);
        }

        return Map.of("prompts", promptList);
    }

    private Map<String, Object> handlePromptsGet(String sessionId, Map<String, Object> params) {
        String promptName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Collections.emptyMap());

        if ("chat".equals(promptName)) {
            String message = (String) arguments.getOrDefault("message", "");
            return buildPromptResult("Chat",
                        "You are a helpful AI assistant. Respond to the following message:\n\n" + message);
        }

        List<McpToolRegistry.McpPrompt> prompts = sessionPrompts.getOrDefault(sessionId, Collections.emptyList());
        for (McpToolRegistry.McpPrompt p : prompts) {
            if (p.getName().equals(promptName)) {
                String rendered = p.render(arguments);
                return buildPromptResult(p.getName(), rendered);
            }
        }

        throw new IllegalArgumentException("Prompt not found: " + promptName);
    }

    // ===== ping =====

    private Object handlePing() {
        return Collections.emptyMap();
    }

    // ===== helpers =====

    private Map<String, Object> buildPromptResult(String name, String content) {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "user");
        Map<String, Object> textContent = new LinkedHashMap<>();
        textContent.put("type", "text");
        textContent.put("text", content);
        msg.put("content", textContent);
        messages.add(msg);

        return Map.of("description", name + " Prompt",
                "messages", messages);
    }

    /**
     * 注册会话资源。
     *
     * @param sessionId 会话 ID
     * @param resource  要注册的 MCP 资源
     */
    public void registerResource(String sessionId, McpToolRegistry.McpResource resource) {
        sessionResources.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(resource);
    }

    /**
     * 注册会话 Prompt。
     *
     * @param sessionId 会话 ID
     * @param prompt    要注册的 MCP Prompt
     */
    public void registerPrompt(String sessionId, McpToolRegistry.McpPrompt prompt) {
        sessionPrompts.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(prompt);
    }
}
