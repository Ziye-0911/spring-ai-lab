package com.liziye.spring.ai.lab.scenario.chat.agent;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.model.AgentContext;
import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.orchestrator.BaseOrchestrator;
import com.liziye.spring.ai.lab.core.routing.ModelProviderManager;
import com.liziye.spring.ai.lab.scenario.chat.ChatAgentProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import java.util.List;
import java.util.Map;

/**
 * 简单对话 Agent 编排器。
 *
 * <p>实现了基础的对话流程：组装 Prompt → 调用模型 → 返回结果。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class SimpleChatAgent extends BaseOrchestrator<AgentContext> {

    private final ChatAgentProperties chatProperties;

    /**
     * 构造 {@link SimpleChatAgent} 实例。
     *
     * @param modelManager  模型提供者管理器
     * @param memory        对话记忆
     * @param advisors      Advisor 列表
     * @param tokenMetrics  Token 用量统计
     * @param latencyMetrics 延迟统计
     * @param chatProperties Chat 场景配置属性
     */
    public SimpleChatAgent(ModelProviderManager modelManager,
                           ConversationMemory memory,
                           List<Advisor> advisors,
                           TokenMetrics tokenMetrics,
                           LatencyMetrics latencyMetrics,
                           ChatAgentProperties chatProperties) {
        super(modelManager, memory, advisors, tokenMetrics, latencyMetrics);
        this.chatProperties = chatProperties;
    }

    @Override
    protected AgentResponse doExecute(ChatClient chatClient, String userInput, AgentContext context) {
        // 构建系统提示
        String systemPrompt = chatProperties.getSystemPrompt();

        // 构建 ChatClient 调用
        String responseText = chatClient.prompt()
                .system(systemPrompt)
                .user(userInput)
                .call()
                .content();

        Map<String, Object> metadata = Map.of(
                "model", "dashscope",
                "timestamp", System.currentTimeMillis()
        );

        return AgentResponse.builder()
                .content(responseText)
                .metadata(metadata)
                .build();
    }

    @Override
    public String name() {
        return "simple-chat-agent";
    }

    @Override
    protected String getOrchestratorName() {
        return "SimpleChatAgent";
    }
}
