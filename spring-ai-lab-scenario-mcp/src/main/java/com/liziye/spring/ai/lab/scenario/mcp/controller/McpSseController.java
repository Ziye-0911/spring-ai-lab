package com.liziye.spring.ai.lab.scenario.mcp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liziye.spring.ai.lab.scenario.mcp.McpProperties;
import com.liziye.spring.ai.lab.scenario.mcp.protocol.McpJsonRpcHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * MCP SSE 传输层控制器。
 *
 * <p>实现 MCP 规范的 SSE 传输：
 * <ul>
 *   <li>GET /mcp/sse — 建立 SSE 连接，获取 sessionId</li>
 *   <li>POST /mcp/message — 发送 JSON-RPC 请求，通过 SSE 推送响应</li>
 * </ul>
 *
 * <p>管理 SSE 会话生命周期，支持心跳保活，当 SSE 推送失败时回退到 HTTP 直接响应。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("${spring.ai.lab.mcp.sse-endpoint:/mcp/sse}")
public class McpSseController {

    private final McpJsonRpcHandler handler;
    private final McpProperties properties;
    private final ObjectMapper objectMapper;

    /** sessionId → SseEmitter */
    private final Map<String, SseEmitter> sessions = new ConcurrentHashMap<>();

    /** 心跳定时器 */
    private final ScheduledExecutorService heartbeatExecutor;

    public McpSseController(McpJsonRpcHandler handler, McpProperties properties) {
        this.handler = handler;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mcp-heartbeat");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 建立 SSE 连接。
     *
     * <p>创建 SSE 长连接并返回端点事件，告知客户端消息端点的 URL。
     * 支持通过 {@code sessionId} 参数复用已有会话。
     *
     * @param sessionId 可选的会话 ID，为空时自动生成
     * @return SSE 事件发射器，首先推送 {@code endpoint} 事件
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam(required = false) String sessionId) {
        String effectiveSessionId = sessionId != null ? sessionId : UUID.randomUUID().toString().substring(0, 8);
        SseEmitter emitter = new SseEmitter(0L); // 超时由调用方管理

        sessions.put(effectiveSessionId, emitter);

        emitter.onCompletion(() -> {
            sessions.remove(effectiveSessionId);
            log.info("[MCP-SSE] connection closed: session={}", effectiveSessionId);
        });
        emitter.onTimeout(() -> {
            sessions.remove(effectiveSessionId);
            log.info("[MCP-SSE] connection timeout: session={}", effectiveSessionId);
        });
        emitter.onError(e -> {
            sessions.remove(effectiveSessionId);
            log.warn("[MCP-SSE] connection error: session={} error={}", effectiveSessionId, e.getMessage());
        });

        try {
            // 发送 endpoint 事件 (MCP 规范)
            String messageEndpoint = properties.getSseMessageEndpoint() +
                    "?sessionId=" + effectiveSessionId;
            emitter.send(SseEmitter.event()
                    .name("endpoint")
                    .data(messageEndpoint));

            log.info("[MCP-SSE] connection established: session={}", effectiveSessionId);

            // 启动心跳
            if (properties.isHeartbeat()) {
                startHeartbeat(effectiveSessionId, emitter);
            }
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void startHeartbeat(String sessionId, SseEmitter emitter) {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                if (sessions.containsKey(sessionId)) {
                    emitter.send(SseEmitter.event()
                            .comment("heartbeat"));
                }
            } catch (Exception e) {
                // 忽略心跳失败
            }
        }, properties.getHeartbeatIntervalSeconds(), properties.getHeartbeatIntervalSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 接收 JSON-RPC 请求。
     *
     * <p>接收客户端发送的 JSON-RPC 请求，通过 {@link McpJsonRpcHandler} 处理，
     * 结果通过 SSE 推送或 HTTP 直接响应（回退模式）。
     *
     * @param sessionId 会话 ID，用于查找对应的 SSE 连接
     * @param request   JSON-RPC 请求体
     * @param response  HTTP 响应对象，用于 SSE 推送失败时的回退
     * @throws IOException 如果 SSE 推送或 HTTP 响应写入失败
     */
    @PostMapping("${spring.ai.lab.mcp.sse-message-endpoint:/mcp/message}")
    public void handleMessage(@RequestParam String sessionId,
                               @RequestBody Map<String, Object> request,
                               HttpServletResponse response) throws IOException {

        log.debug("[MCP-SSE] message received: session={} method={}",
                sessionId, request.get("method"));

        Map<String, Object> result = handler.handleRequest(sessionId, request);

        if (result != null) {
            SseEmitter emitter = sessions.get(sessionId);
            if (emitter != null) {
                try {
                    String json = objectMapper.writeValueAsString(result);
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(json));
                } catch (IOException e) {
                    log.error("[MCP-SSE] failed to send response: session={} error={}",
                            sessionId, e.getMessage());

                    // 回退到 HTTP 200 直接响应
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(objectMapper.writeValueAsString(result));
                }
            } else {
                // 无 SSE 连接时，回退到 HTTP 直接响应
                log.warn("[MCP-SSE] no active SSE connection: session={}, falling back to HTTP", sessionId);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(202);
                response.getWriter().write(objectMapper.writeValueAsString(result));
            }
        }
        // null 表示通知类方法，不需要响应
    }

    /**
     * 获取活跃会话数。
     *
     * @return 当前活跃的 SSE 会话数量
     */
    @GetMapping("/sessions/count")
    public Map<String, Object> getSessionCount() {
        return Map.of("activeSessions", sessions.size());
    }
}
