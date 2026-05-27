package com.liziye.spring.ai.lab.scenario.cs.controller;

import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.model.ApiResult;
import com.liziye.spring.ai.lab.core.model.Message;
import com.liziye.spring.ai.lab.scenario.cs.orchestrator.CustomerServiceOrchestrator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 智能客服 REST 控制器。
 *
 * <p>提供以下 API：
 * <ul>
 *   <li>POST /api/cs/chat — 智能客服对话</li>
 *   <li>POST /api/cs/chat/stream — SSE 流式智能客服</li>
 *   <li>DELETE /api/cs/session/{conversationId} — 清除会话</li>
 *   <li>GET /api/cs/session/{conversationId}/count — 获取会话消息数</li>
 * </ul>
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/cs")
@RequiredArgsConstructor
public class CustomerServiceController {

    private final CustomerServiceOrchestrator orchestrator;

    /**
     * 智能客服对话。
     *
     * <p>接收用户输入，通过 {@link CustomerServiceOrchestrator} 执行意图识别和回复生成。
     *
     * @param request 请求体，包含 {@code conversationId} 和 {@code userInput}
     * @return Agent 响应，包含回复内容、意图标签、置信度等元数据
     */
    @PostMapping("/chat")
    public ApiResult<AgentResponse> chat(@Valid @RequestBody CsChatRequest request) {
        log.info("[CS-API] chat: conversation={} input={}",
                request.getConversationId(),
                truncate(request.getUserInput(), 50));

        AgentResponse response = orchestrator.execute(
                request.getConversationId(),
                request.getUserInput());

        if (!response.isFallback()) {
            return ApiResult.success(response);
        } else {
            return ApiResult.error(500, response.getContent());
        }
    }

    /**
     * 清除会话。
     *
     * @param conversationId 会话 ID
     * @return 操作结果，包含会话 ID 和操作类型（cleared）
     */
    @DeleteMapping("/session/{conversationId}")
    public ApiResult<Map<String, Object>> clearSession(
            @PathVariable String conversationId) {
        orchestrator.clearSession(conversationId);
        log.info("[CS-API] Session cleared: conversation={}", conversationId);
        return ApiResult.success(Map.of(
                "conversationId", conversationId,
                "action", "cleared"
        ));
    }

    /**
     * 获取会话消息数。
     *
     * @param conversationId 会话 ID
     * @return 会话 ID 和当前消息数量
     */
    @GetMapping("/session/{conversationId}/count")
    public ApiResult<Map<String, Object>> getMessageCount(
            @PathVariable String conversationId) {
        int count = orchestrator.getMessageCount(conversationId);
        return ApiResult.success(Map.of(
                "conversationId", conversationId,
                "messageCount", count
        ));
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    @Data
    public static class CsChatRequest {
        @NotBlank(message = "conversationId 不能为空")
        private String conversationId;

        @NotBlank(message = "userInput 不能为空")
        private String userInput;
    }
}
