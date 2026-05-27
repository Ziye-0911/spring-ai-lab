package com.liziye.spring.ai.lab.scenario.multiagent.model;

import com.liziye.spring.ai.lab.core.model.AgentContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 多 Agent 协作专属上下文。
 *
 * <p>继承自 {@link AgentContext}，扩展了协作模式、Agent 角色定义、
 * 迭代控制等字段，用于在多 Agent 协作场景中传递上下文信息。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MultiAgentContext extends AgentContext {

    /** 协作模式：sequential / parallel / router / debate */
    private String collaborationMode = "sequential";

    /** Agent 角色定义列表 */
    private List<AgentRole> agentRoles = new ArrayList<>();

    /** Agent 执行链（顺序模式） */
    private List<String> agentChain = new ArrayList<>();

    /** 最大迭代轮次 */
    private int maxIterations = 3;

    /** 当前迭代轮次 */
    private int currentIteration = 0;

    /** 允许的最大 Agent 数量 */
    private int maxAgents = 5;

    /** 任务超时时间（秒） */
    private int taskTimeoutSeconds = 120;

    /** 是否启用辩论回合 */
    private boolean enableDebate = false;

    /** 辩论最大回合数 */
    private int maxDebateRounds = 2;

    /**
     * Agent 角色定义。
     *
     * <p>定义协作中每个 Agent 的名称、描述、系统提示和可用工具。
     */
    @Data
    public static class AgentRole {
        /** 角色名称（如 "架构师"、"编码专家"、"安全审查员"） */
        private String name;

        /** 角色描述 */
        private String description;

        /** 角色系统提示 */
        private String systemPrompt;

        /** 工具集合 */
        private List<String> tools = new ArrayList<>();
    }
}
