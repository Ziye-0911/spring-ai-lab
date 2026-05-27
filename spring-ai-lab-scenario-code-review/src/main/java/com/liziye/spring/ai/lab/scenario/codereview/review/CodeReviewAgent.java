package com.liziye.spring.ai.lab.scenario.codereview.review;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.orchestrator.BaseOrchestrator;
import com.liziye.spring.ai.lab.core.routing.ModelProviderManager;
import com.liziye.spring.ai.lab.scenario.codereview.CodeReviewProperties;
import com.liziye.spring.ai.lab.scenario.codereview.git.GitDiffParser;
import com.liziye.spring.ai.lab.scenario.codereview.model.CodeReviewContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import java.util.List;
import java.util.Map;

/**
 * 代码审查 Agent 编排器。
 *
 * <p>继承 {@link BaseOrchestrator}，实现了代码 Diff 解析、审查 Prompt 构建和结果生成。
 * 支持对 Git Diff 和代码片段进行多维度审查。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class CodeReviewAgent extends BaseOrchestrator<CodeReviewContext> {

    private final CodeReviewProperties reviewProperties;
    private final GitDiffParser diffParser;

    /**
     * 构造 {@link CodeReviewAgent} 实例。
     *
     * @param modelManager     模型提供者管理器
     * @param memory           对话记忆
     * @param advisors         Advisor 列表
     * @param tokenMetrics     Token 用量统计
     * @param latencyMetrics    延迟统计
     * @param reviewProperties 代码审查配置属性
     */
    public CodeReviewAgent(ModelProviderManager modelManager,
                           ConversationMemory memory,
                           List<Advisor> advisors,
                           TokenMetrics tokenMetrics,
                           LatencyMetrics latencyMetrics,
                           CodeReviewProperties reviewProperties) {
        super(modelManager, memory, advisors, tokenMetrics, latencyMetrics);
        this.reviewProperties = reviewProperties;
        this.diffParser = new GitDiffParser();
    }

    @Override
    protected AgentResponse doExecute(ChatClient chatClient, String userInput, CodeReviewContext context) {
        String diffContent = context.getDiffContent();

        if (diffContent == null || diffContent.trim().isEmpty()) {
            return AgentResponse.builder()
                    .content("未提供代码变更内容，无法进行审查。请通过 POST /api/code-review/submit 提交 Diff。")
                    .metadata(Map.of("status", "no_diff"))
                    .build();
        }

        // 解析 Diff
        List<GitDiffParser.FileChange> changes = diffParser.parse(diffContent);
        String changeSummary = diffParser.extractSummary(changes);

        // 限制 Diff 大小
        int maxLines = reviewProperties.getMaxDiffLines();
        if (diffContent.split("\n").length > maxLines) {
            log.warn("[CODE-REVIEW] Diff too large, truncating to {} lines", maxLines);
            String[] lines = diffContent.split("\n");
            diffContent = String.join("\n",
                    java.util.Arrays.copyOf(lines, maxLines)) + "\n... (truncated)";
        }

        // 构建审查 Prompt
        String reviewPrompt = buildReviewPrompt(diffContent, changeSummary, context);

        // 调用模型
        String reviewResult = chatClient.prompt()
                .system(reviewProperties.getSystemPrompt())
                .user(reviewPrompt)
                .call()
                .content();

        return AgentResponse.builder()
                .content(reviewResult)
                .metadata(Map.of(
                        "model", "dashscope",
                        "filesReviewed", changes.size(),
                        "totalAdditions", changes.stream().mapToInt(GitDiffParser.FileChange::getAdditions).sum(),
                        "totalDeletions", changes.stream().mapToInt(GitDiffParser.FileChange::getDeletions).sum(),
                        "changeSummary", changeSummary
                ))
                .build();
    }

    private String buildReviewPrompt(String diffContent, String changeSummary, CodeReviewContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 代码审查请求\n\n");

        sb.append("## 变更摘要\n");
        sb.append(changeSummary).append("\n\n");

        if (context.getLanguage() != null) {
            sb.append("## 语言\n").append(context.getLanguage()).append("\n\n");
        }

        if (context.isCriticalOnly()) {
            sb.append("## 审查范围: 仅关键问题\n\n");
        }

        sb.append("## 代码变更 (Diff)\n```diff\n");
        sb.append(diffContent);
        sb.append("\n```\n\n");

        sb.append("请对以上代码变更进行全面审查。");

        return sb.toString();
    }

    @Override
    public String name() {
        return "code-review-agent";
    }

    @Override
    protected String getOrchestratorName() {
        return "CodeReviewAgent";
    }
}
