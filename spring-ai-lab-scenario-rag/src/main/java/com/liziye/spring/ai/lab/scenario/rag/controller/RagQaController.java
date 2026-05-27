package com.liziye.spring.ai.lab.scenario.rag.controller;

import com.liziye.spring.ai.lab.core.model.ApiResult;
import com.liziye.spring.ai.lab.core.model.AgentRequest;
import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.scenario.rag.model.RagAgentContext;
import com.liziye.spring.ai.lab.scenario.rag.orchestrator.RagAgentOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

/**
 * RAG 知识库问答 REST 控制器。
 *
 * <p>提供同步和流式 RAG 问答接口：
 * <ul>
 *   <li>POST /api/rag/ask — 同步问答</li>
 *   <li>POST /api/rag/ask/stream — SSE 流式问答</li>
 *   <li>GET /api/rag/config — 查询 Agent 配置</li>
 * </ul>
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagQaController {

    private final RagAgentOrchestrator orchestrator;

    /**
     * 同步问答。
     *
     * <p>接收用户问题，创建 {@link RagAgentContext} 上下文，
     * 通过 {@link RagAgentOrchestrator} 检索知识库并生成回答。
     *
     * @param request 请求体，包含用户输入和可选的会话 ID
     * @return Agent 响应，包含回答内容和元数据
     */
    @PostMapping("/ask")
    public ApiResult<AgentResponse> ask(@RequestBody AgentRequest request) {
        String conversationId = request.getConversationId() != null
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        log.info("[RAG-ASK] conversation={} input={}", conversationId, request.getUserInput());

        RagAgentContext context = new RagAgentContext();
        context.setConversationId(conversationId);
        context.setModelProvider("dashscope");
        context.setTopK(5);
        context.setSimilarityThreshold(0.7);

        AgentResponse response = orchestrator.execute(conversationId, request.getUserInput(), context);
        return ApiResult.success(response);
    }

    /**
     * 流式问答（SSE）。
     *
     * <p>通过 Server-Sent Events 流式返回回答内容。
     *
     * @param request 请求体，包含用户输入和可选的会话 ID
     * @return SSE 事件流，每个事件包含一段回答内容
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@RequestBody AgentRequest request) {
        String conversationId = request.getConversationId() != null
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        RagAgentContext context = new RagAgentContext();
        context.setConversationId(conversationId);
        context.setModelProvider("dashscope");
        context.setTopK(5);
        context.setSimilarityThreshold(0.7);

        return orchestrator.executeStream(conversationId, request.getUserInput(), context)
                .map(response -> "event: message\ndata: " + response.getContent() + "\n\n");
    }

    /**
     * 查询当前 RAG Agent 配置。
     *
     * @return Agent 名称和可用工具列表
     */
    @GetMapping("/config")
    public ApiResult<Map<String, Object>> config() {
        return ApiResult.success(Map.of(
                "agent", orchestrator.name(),
                "availableTools", orchestrator.getAvailableTools()
        ));
    }
}
