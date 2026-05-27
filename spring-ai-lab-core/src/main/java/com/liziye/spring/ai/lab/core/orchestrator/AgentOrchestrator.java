package com.liziye.spring.ai.lab.core.orchestrator;

import com.liziye.spring.ai.lab.core.model.AgentContext;
import com.liziye.spring.ai.lab.core.model.AgentResponse;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Agent 编排器接口 — 增加泛型参数，实现类型安全的上下文传递。
 *
 * <p>设计思路：
 * <ul>
 *   <li>{@code T extends AgentContext}：每个场景定义自己的 Context 子类</li>
 *   <li>{@code RagAgentContext} → topK, rerank, similarityThreshold</li>
 *   <li>{@code MultiAgentContext} → collaborationMode, agentChain, maxIterations</li>
 *   <li>{@code CodeReviewContext} → reviewCategories, diffSource</li>
 * </ul>
 *
 * @param <T> 场景专属的 Context 类型
 * @author liziye
 * @since 1.0.0
 */
public interface AgentOrchestrator<T extends AgentContext> {

    /**
     * 同步执行一个 Agent 任务。
     *
     * @param conversationId 会话ID
     * @param userInput      用户输入
     * @param context        场景专属上下文（类型安全）
     * @return Agent 执行结果
     */
    AgentResponse execute(String conversationId, String userInput, T context);

    /**
     * 流式执行 Agent 任务。
     *
     * @param conversationId 会话ID
     * @param userInput      用户输入
     * @param context        场景专属上下文
     * @return Flux 流，逐块推送 AgentResponse
     */
    Flux<AgentResponse> executeStream(String conversationId, String userInput, T context);

    /** 获取编排器名称 */
    String name();

    /** 获取该编排器拥有的工具列表 */
    List<String> getAvailableTools();
}
