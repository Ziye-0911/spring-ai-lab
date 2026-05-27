package com.liziye.spring.ai.lab.scenario.multiagent.orchestrator;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.orchestrator.BaseOrchestrator;
import com.liziye.spring.ai.lab.core.routing.ModelProviderManager;
import com.liziye.spring.ai.lab.scenario.multiagent.MultiAgentProperties;
import com.liziye.spring.ai.lab.scenario.multiagent.mode.DebateMode;
import com.liziye.spring.ai.lab.scenario.multiagent.mode.ParallelMode;
import com.liziye.spring.ai.lab.scenario.multiagent.mode.RouterMode;
import com.liziye.spring.ai.lab.scenario.multiagent.mode.SequentialMode;
import com.liziye.spring.ai.lab.scenario.multiagent.model.MultiAgentContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import java.util.List;
import java.util.Map;

/**
 * 多 Agent 协作编排器。
 *
 * <p>继承 {@link BaseOrchestrator}，只需实现多 Agent 调度逻辑。
 * 支持四种协作模式：顺序（sequential）、并行（parallel）、路由（router）、辩论（debate）。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class MultiAgentOrchestrator extends BaseOrchestrator<MultiAgentContext> {

    private final MultiAgentProperties properties;

    /**
     * 构造 {@link MultiAgentOrchestrator} 实例。
     *
     * @param modelManager  模型提供者管理器
     * @param memory        对话记忆
     * @param advisors      Advisor 列表
     * @param tokenMetrics  Token 用量统计
     * @param latencyMetrics 延迟统计
     * @param properties    多 Agent 配置属性
     */
    public MultiAgentOrchestrator(ModelProviderManager modelManager,
                                   ConversationMemory memory,
                                   List<Advisor> advisors,
                                   TokenMetrics tokenMetrics,
                                   LatencyMetrics latencyMetrics,
                                   MultiAgentProperties properties) {
        super(modelManager, memory, advisors, tokenMetrics, latencyMetrics);
        this.properties = properties;
    }

    @Override
    protected AgentResponse doExecute(ChatClient chatClient, String userInput, MultiAgentContext context) {
        // 确保有默认的 Agent 角色
        if (context.getAgentRoles() == null || context.getAgentRoles().isEmpty()) {
            context.setAgentRoles(buildDefaultRoles());
        }

        String mode = context.getCollaborationMode() != null
                ? context.getCollaborationMode()
                : properties.getDefaultMode();

        log.info("[MULTI-AGENT] mode={} agents={} input_preview={}",
                mode, context.getAgentRoles().size(), truncate(userInput, 100));

        SequentialMode.AgentRunner runner = this::runAgent;

        String result = switch (mode.toLowerCase()) {
            case "parallel" -> ParallelMode.execute(chatClient, userInput, context, runner);
            case "router" -> RouterMode.execute(chatClient, userInput, context, runner);
            case "debate" -> DebateMode.execute(chatClient, userInput, context, runner);
            default -> SequentialMode.execute(chatClient, userInput, context, runner);
        };

        return AgentResponse.builder()
                .content(result)
                .metadata(Map.of(
                        "model", "dashscope",
                        "mode", mode,
                        "agents", context.getAgentRoles().size(),
                        "agentNames", context.getAgentRoles().stream()
                                .map(MultiAgentContext.AgentRole::getName)
                                .toList()
                ))
                .build();
    }

    /**
     * 单个 Agent 的执行逻辑。
     */
    private String runAgent(ChatClient chatClient, MultiAgentContext.AgentRole role, String taskInput) {
        try {
            String systemPrompt = role.getSystemPrompt() != null
                    ? role.getSystemPrompt()
                    : properties.getSystemPrompt();

            return chatClient.prompt()
                    .system(systemPrompt + "\n\n当前角色: " + role.getName())
                    .user(taskInput)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("[MULTI-AGENT] Agent {} failed: {}", role.getName(), e.getMessage());
            return "（Agent " + role.getName() + " 执行失败: " + e.getMessage() + "）";
        }
    }

    /**
     * 构建默认的 Agent 角色列表。
     */
    private List<MultiAgentContext.AgentRole> buildDefaultRoles() {
        return List.of(
                createRole("需求分析师",
                        "专注于理解用户需求，将模糊描述转化为清晰的技术需求",
                        "你是一位资深的【需求分析师】，擅长从用户描述中提取核心需求和边界条件。"),
                createRole("架构设计师",
                        "专注于系统架构设计，技术选型，系统解耦",
                        "你是一位资深的【架构设计师】，擅长设计高可用、可扩展的系统架构。"),
                createRole("代码审查员",
                        "专注于代码质量、安全性和最佳实践",
                        "你是一位资深的【代码审查员】，严谨审查代码质量和安全性。")
        );
    }

    private MultiAgentContext.AgentRole createRole(String name, String description, String systemPrompt) {
        MultiAgentContext.AgentRole role = new MultiAgentContext.AgentRole();
        role.setName(name);
        role.setDescription(description);
        role.setSystemPrompt(systemPrompt);
        return role;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "null";
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }

    @Override
    public String name() {
        return "multi-agent-orchestrator";
    }

    @Override
    protected String getOrchestratorName() {
        return "MultiAgentOrchestrator";
    }
}
