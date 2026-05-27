package com.liziye.spring.ai.lab.scenario.multiagent.controller;

import com.liziye.spring.ai.lab.core.model.AgentRequest;
import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.model.ApiResult;
import com.liziye.spring.ai.lab.scenario.multiagent.model.MultiAgentContext;
import com.liziye.spring.ai.lab.scenario.multiagent.orchestrator.MultiAgentOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 多 Agent 协作 REST 控制器。
 *
 * <p>提供多 Agent 协作任务的执行、流式输出、模式查询和健康检测接口。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/multi-agent")
@RequiredArgsConstructor
public class MultiAgentController {

    private final MultiAgentOrchestrator orchestrator;

    /**
     * 执行多 Agent 协作任务。
     *
     * <p>POST {@code /api/multi-agent/execute}
     *
     * <p>请求体示例：
     * <pre>{@code
     * {
     *   "userInput": "帮我设计一个电商系统",
     *   "mode": "sequential",
     *   "agents": [
     *     {"name": "需求分析师", "description": "...", "systemPrompt": "..."},
     *     {"name": "架构设计师", "description": "...", "systemPrompt": "..."}
     *   ]
     * }
     * }</pre>
     *
     * @param request 请求参数，包含 userInput、mode(可选)、agents(可选)
     * @return 多 Agent 协作结果
     */
    @PostMapping("/execute")
    public ApiResult<AgentResponse> execute(@RequestBody Map<String, Object> request) {
        String userInput = (String) request.get("userInput");
        String mode = (String) request.getOrDefault("mode", "sequential");
        String conversationId = UUID.randomUUID().toString();

        MultiAgentContext context = new MultiAgentContext();
        context.setConversationId(conversationId);
        context.setModelProvider("dashscope");
        context.setCollaborationMode(mode);

        // 解析自定义 Agent 角色
        @SuppressWarnings("unchecked")
        List<Map<String, String>> agents = (List<Map<String, String>>) request.get("agents");
        if (agents != null && !agents.isEmpty()) {
            for (Map<String, String> agentMap : agents) {
                MultiAgentContext.AgentRole role = new MultiAgentContext.AgentRole();
                role.setName(agentMap.getOrDefault("name", "专家"));
                role.setDescription(agentMap.getOrDefault("description", "AI 专家"));
                role.setSystemPrompt(agentMap.get("systemPrompt"));
                context.getAgentRoles().add(role);
            }
        }

        log.info("[MULTI-AGENT] conversation={} mode={} agents={} input={}",
                conversationId, mode,
                context.getAgentRoles().isEmpty() ? "default" : context.getAgentRoles().size(),
                userInput);

        AgentResponse response = orchestrator.execute(conversationId, userInput, context);
        return ApiResult.success(response);
    }

    /**
     * 流式多 Agent 协作。
     *
     * <p>POST {@code /api/multi-agent/execute/stream}
     *
     * @param request 对话请求
     * @return SSE 事件流
     */
    @PostMapping(value = "/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeStream(@RequestBody AgentRequest request) {
        return Flux.just("data: " + "流式多Agent协作待实现" + "\n\n");
    }

    /**
     * 查询支持的协作模式。
     *
     * <p>GET {@code /api/multi-agent/modes}
     *
     * @return 可用协作模式列表
     */
    @GetMapping("/modes")
    public ApiResult<Map<String, Object>> modes() {
        return ApiResult.success(Map.of(
                "availableModes", List.of(
                        Map.of("mode", "sequential", "description", "顺序执行：Agent 按顺序依次执行"),
                        Map.of("mode", "parallel", "description", "并行执行：多个 Agent 同时并行执行"),
                        Map.of("mode", "router", "description", "路由模式：自动选择最合适的 Agent"),
                        Map.of("mode", "debate", "description", "辩论模式：Agent 互相辩论达成共识")
                ),
                "defaultMode", "sequential"
        ));
    }

    /**
     * 健康检查。
     *
     * <p>GET {@code /api/multi-agent/health}
     *
     * @return 服务健康状态
     */
    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        return ApiResult.success(Map.of(
                "status", "UP",
                "agent", orchestrator.name(),
                "availableTools", orchestrator.getAvailableTools()
        ));
    }
}
