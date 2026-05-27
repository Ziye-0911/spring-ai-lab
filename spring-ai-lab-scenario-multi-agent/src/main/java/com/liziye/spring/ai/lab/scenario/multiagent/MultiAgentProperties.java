package com.liziye.spring.ai.lab.scenario.multiagent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多 Agent 协作场景配置属性。
 *
 * <p>绑定 {@code spring.ai.lab.multi-agent} 前缀的配置项，支持自定义协作模式、
 * 迭代轮次、超时时间等参数。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.lab.multi-agent")
public class MultiAgentProperties {

    /** 默认协作模式：sequential / parallel / router / debate */
    private String defaultMode = "sequential";

    /** 最大迭代轮次 */
    private int maxIterations = 3;

    /** 最大 Agent 数量 */
    private int maxAgents = 5;

    /** 默认温度 */
    private double temperature = 0.7;

    /** 默认最大 Token */
    private int maxTokens = 4096;

    /** 任务超时时间（秒） */
    private int taskTimeoutSeconds = 120;

    /** 系统提示 */
    private String systemPrompt = """
            你是一个多角色 AI 协作系统中的专家 Agent。
                        
            协作规则：
            1. 请基于你的角色分工给出专业意见
            2. 引用其他 Agent 的分析时请指明来源
            3. 如有分歧请客观分析各方观点
            4. 最终综合所有分析给出总结
            """;
}
