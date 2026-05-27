package com.liziye.spring.ai.lab.scenario.chat.controller;

import com.liziye.spring.ai.lab.core.model.AgentContext;
import com.liziye.spring.ai.lab.core.model.AgentRequest;
import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.model.ApiResult;
import com.liziye.spring.ai.lab.scenario.chat.agent.SimpleChatAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

/**
 * Chat Agent REST 控制器。
 *
 * <p>提供同步和流式两种对话接口，支持多轮对话和健康检测。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SimpleChatAgent simpleChatAgent;

    /**
     * 同步对话接口。
     *
     * <p>POST {@code /api/chat}
     *
     * <p>请求体示例：
     * <pre>{@code
     * {
     *   "conversationId": "可选，不传则自动生成",
     *   "userInput": "用户消息",
     *   "modelName": "可选，默认 dashscope"
     * }
     * }</pre>
     *
     * @param request 对话请求
     * @return 对话结果
     */
    @PostMapping
    public ApiResult<AgentResponse> chat(@RequestBody AgentRequest request) {
        String conversationId = request.getConversationId() != null
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        log.info("[CHAT] conversation={} input={}", conversationId, request.getUserInput());

        AgentContext context = new AgentContext();
        context.setConversationId(conversationId);
        context.setModelProvider(request.getModelName() != null
                ? request.getModelName() : "dashscope");

        AgentResponse response = simpleChatAgent.execute(conversationId, request.getUserInput(), context);

        log.info("[CHAT] conversation={} response_length={}", conversationId,
                response.getContent() != null ? response.getContent().length() : 0);

        return ApiResult.success(response);
    }

    /**
     * 流式对话接口（SSE）。
     *
     * <p>POST {@code /api/chat/stream}
     *
     * <p>请求体同 {@code /api/chat}，响应为 {@code text/event-stream} 格式。
     *
     * @param request 对话请求
     * @return SSE 事件流
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody AgentRequest request) {
        String conversationId = request.getConversationId() != null
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        log.info("[CHAT-STREAM] conversation={} input={}", conversationId, request.getUserInput());

        AgentContext context = new AgentContext();
        context.setConversationId(conversationId);
        context.setModelProvider(request.getModelName() != null
                ? request.getModelName() : "dashscope");

        // 发送开始事件
        Flux<String> startEvent = Flux.just("event: start\ndata: {\"conversationId\": \"" + conversationId + "\"}\n\n");

        // 流式输出
        Flux<String> streamEvents = simpleChatAgent.executeStream(conversationId, request.getUserInput(), context)
                .map(response -> "event: message\ndata: " + response.getContent() + "\n\n");

        // 发送完成事件
        Flux<String> endEvent = Flux.just("event: done\ndata: {\"conversationId\": \"" + conversationId + "\"}\n\n");

        return Flux.concat(startEvent, streamEvents, endEvent);
    }

    /**
     * 健康检测接口。
     *
     * <p>GET {@code /api/chat/health}
     *
     * @return 服务健康状态
     */
    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        return ApiResult.success(Map.of(
                "status", "UP",
                "agent", simpleChatAgent.name(),
                "availableTools", simpleChatAgent.getAvailableTools()
        ));
    }
}
