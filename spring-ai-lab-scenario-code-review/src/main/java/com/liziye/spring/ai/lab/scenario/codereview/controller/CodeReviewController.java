package com.liziye.spring.ai.lab.scenario.codereview.controller;

import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.model.ApiResult;
import com.liziye.spring.ai.lab.scenario.codereview.model.CodeReviewContext;
import com.liziye.spring.ai.lab.scenario.codereview.review.CodeReviewAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 代码审查 REST 控制器。
 *
 * <p>提供代码 Diff 审查、代码片段审查和健康检测接口。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/code-review")
@RequiredArgsConstructor
public class CodeReviewController {

    private final CodeReviewAgent codeReviewAgent;

    /**
     * 提交代码审查请求。
     *
     * <p>POST {@code /api/code-review/submit}
     *
     * <p>请求体示例：
     * <pre>{@code
     * {
     *   "diffContent": "代码 Diff 内容",
     *   "language": "java",
     *   "criticalOnly": false
     * }
     * }</pre>
     *
     * @param request 请求参数，包含 diffContent、language(可选)、criticalOnly(可选)
     * @return 审查结果
     */
    @PostMapping("/submit")
    public ApiResult<AgentResponse> submitReview(@RequestBody Map<String, Object> request) {
        String diffContent = (String) request.get("diffContent");
        String language = (String) request.getOrDefault("language", "java");
        Boolean criticalOnly = (Boolean) request.getOrDefault("criticalOnly", false);

        String conversationId = UUID.randomUUID().toString();

        log.info("[CODE-REVIEW] Starting review: language={}, diffLength={}", language,
                diffContent != null ? diffContent.length() : 0);

        CodeReviewContext context = new CodeReviewContext();
        context.setConversationId(conversationId);
        context.setModelProvider("dashscope");
        context.setDiffContent(diffContent);
        context.setDiffSource("manual");
        context.setLanguage(language);
        context.setCriticalOnly(criticalOnly);

        AgentResponse response = codeReviewAgent.execute(conversationId, diffContent, context);

        log.info("[CODE-REVIEW] Review completed: files={}",
                response.getMetadata() != null ? response.getMetadata().get("filesReviewed") : 0);

        return ApiResult.success(response);
    }

    /**
     * 快速代码片段审查。
     *
     * <p>POST {@code /api/code-review/snippet}
     *
     * <p>请求体示例：
     * <pre>{@code
     * {
     *   "code": "代码片段",
     *   "language": "java"
     * }
     * }</pre>
     *
     * @param request 请求参数，包含 code、language(可选)
     * @return 审查结果
     */
    @PostMapping("/snippet")
    public ApiResult<AgentResponse> reviewSnippet(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String language = request.getOrDefault("language", "java");

        String conversationId = UUID.randomUUID().toString();

        CodeReviewContext context = new CodeReviewContext();
        context.setConversationId(conversationId);
        context.setModelProvider("dashscope");
        context.setDiffSource("snippet");
        context.setLanguage(language);

        AgentResponse response = codeReviewAgent.execute(conversationId, code, context);
        return ApiResult.success(response);
    }

    /**
     * 健康检测。
     *
     * <p>GET {@code /api/code-review/health}
     *
     * @return 服务健康状态
     */
    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        return ApiResult.success(Map.of(
                "status", "UP",
                "agent", codeReviewAgent.name(),
                "availableTools", codeReviewAgent.getAvailableTools()
        ));
    }
}
